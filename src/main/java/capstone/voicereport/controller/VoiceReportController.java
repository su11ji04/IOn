package capstone.voicereport.controller;

import capstone.voicereport.dto.PagedListResponse;
import capstone.voicereport.dto.VoiceReportListResponse;
import capstone.voicereport.dto.VoiceReportResponse;
import capstone.voicereport.error.VoiceReportException;
import capstone.voicereport.service.VoiceReportService;
import capstone.web.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/voice-reports")
public class VoiceReportController {

    private final VoiceReportService voiceReportService;
    private final CurrentUser currentUser;

    // 보이스 리포트 생성
//    @PostMapping(
//            value = "/{userId}",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
//            produces = MediaType.APPLICATION_JSON_VALUE
//    )
//    public ResponseEntity<VoiceReportResponse> createVoiceReport(
//            @PathVariable("userId") int userId,
//            @RequestPart("video") MultipartFile video,
//            HttpServletRequest request
//    ) {
//        VoiceReportResponse resp = voiceReportService.createVoiceReportFromVideo(userId, video);
//        return ResponseEntity.ok(resp);
//    }

    // 보이스리포트 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<VoiceReportResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(voiceReportService.get(id));
    }

    // 보이스리포트 목록 조회
    @GetMapping
    public ResponseEntity<PagedListResponse<VoiceReportListResponse>> list(
            @RequestParam("userId") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        userId = "u001";
        Page<VoiceReportListResponse> p = voiceReportService.list(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(
                new PagedListResponse<>(p.getContent(), p.getTotalPages(), p.getSize())
        );
    }


}