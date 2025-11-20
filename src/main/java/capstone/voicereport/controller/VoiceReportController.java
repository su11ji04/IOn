package capstone.voicereport.controller;

import capstone.voicereport.dto.PagedListResponse;
import capstone.voicereport.dto.VoiceReportListResponse;
import capstone.voicereport.dto.VoiceReportResponse;
import capstone.voicereport.dto.VoiceReportSummaryDto;
import capstone.voicereport.error.VoiceReportException;
import capstone.voicereport.service.VoiceReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/voice-reports")
public class VoiceReportController {

    private final VoiceReportService voiceReportService;

    // 보이스리포트 생성
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<VoiceReportResponse> create(
            @RequestPart("video") MultipartFile video,
            HttpSession session,
            HttpServletRequest request
    ) {
        Integer sessionId = (Integer) session.getAttribute("userId");
        if (sessionId == null) throw VoiceReportException.loginRequired();
        if (video == null || video.isEmpty()) throw VoiceReportException.payloadEmpty();

        VoiceReportResponse resp = voiceReportService.createVoiceReportFromVideo(sessionId, video);
        URI location = URI.create(request.getRequestURI() + "/" + resp.getReportId());
        return ResponseEntity.created(location).body(resp);
    }

    // 보이스리포트 단건 조회
    @GetMapping("/{reportId}")
    public ResponseEntity<VoiceReportResponse> get(
            @PathVariable int reportId,
            HttpSession session
    ) {
        Integer sessionId = (Integer) session.getAttribute("userId");
        if (sessionId == null) throw VoiceReportException.loginRequired();
        return ResponseEntity.ok(voiceReportService.getOneVoicereport(sessionId, reportId));
    }

    // 보이스리포트 목록 조회
    @GetMapping("/list")
    public ResponseEntity<PagedListResponse<VoiceReportListResponse>> list(HttpSession session) {
        Integer sessionId = (Integer) session.getAttribute("userId");
        if (sessionId == null) throw VoiceReportException.loginRequired();
        int page = 0;
        int size = 10;

        Page<VoiceReportListResponse> p = voiceReportService.list(sessionId, PageRequest.of(page, size));
        return ResponseEntity.ok(
                new PagedListResponse<>(p.getContent(), p.getTotalPages(), p.getSize())
        );
    }

    // 보이스리포트 요약 조회
    @GetMapping("/recentSummary")
    public ResponseEntity<VoiceReportSummaryDto> get(HttpSession session) {
        Integer sessionId = (Integer) session.getAttribute("userId");
        if (sessionId == null) throw VoiceReportException.loginRequired();
        return ResponseEntity.ok(voiceReportService.getSummary(sessionId));
    }


}