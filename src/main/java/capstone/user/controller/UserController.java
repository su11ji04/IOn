package capstone.user.controller;

import capstone.user.service.UserService;
import capstone.user.dto.*;
import capstone.workbook.service.WorkbookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import capstone.user.dto.UserRegisterDto;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static capstone.user.error.UserException.loginBadCredentials;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/membership")
public class UserController {

    private final UserService userService;
    private final WorkbookService workbookService;

    // 회원가입
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserIdDto> register(
            @RequestPart("user") String userJson,
            @RequestPart(value = "user_image", required = false) MultipartFile user_image
    ) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        UserRegisterDto req = mapper.readValue(userJson, UserRegisterDto.class);

        // 1) 회원가입 진행 (트랜잭션 내부)
        UserIdDto dto = userService.register(req, user_image);

        // 2) 트랜잭션이 끝난 후에 Python 호출 (커밋 완료 상태)
        workbookService.createCustomWorkbook(dto.getUserId());
        workbookService.createCustomSimulations(dto.getUserId());

        return ResponseEntity.ok(dto);
    }



    // 유형검사 문항 보내기
    @GetMapping
    public ResponseEntity<List<PropensityTestContentDto>> propensityTestContent() {
        return ResponseEntity.ok(userService.getAllTests());
    }

    //회원 정보 확인
    @GetMapping("/{userId}")
    public ResponseEntity<UserInformationCheckDto> checkInformation(@PathVariable int userId) {
        return ResponseEntity.ok(userService.getUserInformationById(userId));
    }

    // 회원 정보 수정
    @PostMapping(
            value = "/modifyUserInformation",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> modifyUserInformation(
            HttpSession session,
            @RequestPart(value = "user", required = false) Part userPart,
            @RequestParam(value = "user", required = false) String userParam,
            @RequestPart(value = "user_image", required = false) MultipartFile user_image
    ) throws Exception {
        Integer sessionId = (Integer) session.getAttribute("userId");
        if (sessionId == null) throw loginBadCredentials();

        String userJson = (userParam != null)
                ? userParam
                : (userPart != null ? new String(userPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8) : null);

        if (userJson == null || userJson.isBlank()) {
            throw new IllegalArgumentException("user 파트(JSON)가 누락되었습니다.");
        }

        UserInformationModifyDto req = new ObjectMapper().readValue(userJson, UserInformationModifyDto.class);
        userService.modifyInformation(sessionId, req, user_image);
        return ResponseEntity.ok().build();
    }

    //유형검사 결과 조회
    @GetMapping("/test-result/{userId}")
    public ResponseEntity<PropensityTestResultDto> getTestResult(@PathVariable int userId) {
        return ResponseEntity.ok(userService.getTestResultByUserId(userId));
    }


    //로그인
    @PostMapping("/login")
    public ResponseEntity<SessionDto> login(
            @RequestBody AuthRequestDto req,
            HttpServletRequest request,
            HttpServletResponse response) {

        SessionDto sessionDto = userService.login(req, request, response);
        return ResponseEntity.ok(sessionDto);
    }


}
