package capstone.workbook.controller;

import capstone.voicereport.dto.PagedListResponse;
import capstone.voicereport.dto.VoiceReportListResponse;
import capstone.web.CurrentUser;
import capstone.workbook.dto.WorkbookListResponse;
import capstone.workbook.service.WorkbookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*") //개발용
@RestController
@RequestMapping("/api/workbooks")
@RequiredArgsConstructor
public class WorkbookController {

    private final WorkbookService service;
    private final CurrentUser currentUser;

    // 워크북 생성 + 저장
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String,Object>> create(HttpServletRequest req) {
        String userId = currentUser.getUserId(req);
        Long id = service.createAndSave(userId);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // 워크북 목록 조회
    @GetMapping
    public ResponseEntity<PagedListResponse<WorkbookListResponse>> list(
            @RequestParam("userId") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Page<WorkbookListResponse> p = service.list(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(
                new PagedListResponse<>(p.getContent(), p.getTotalPages(), p.getSize())
        );
    }

}