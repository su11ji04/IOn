package capstone.voicereport.controller;

import capstone.voicereport.service.VoiceReportResponse;
import capstone.voicereport.service.VoiceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/voice-reports")
public class VoiceReportController {

    private final VoiceReportService voiceReportService;

    // 음성 파일 -> VOICEREPORT CONTROLLER
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceReportResponse> create(
            @RequestPart("audio") MultipartFile audio
    ) throws IOException {
        log.info("[VOICEREPORT CONTROLLER] received audio: name={}, size={}, contentType={}",
                audio.getOriginalFilename(), audio.getSize(), audio.getContentType());
        VoiceReportResponse res = voiceReportService.createVoiceReport(audio);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // 보이스리포트 조회 (BY VOICEREPORT ID)
    @GetMapping("/{id}")
    public ResponseEntity<VoiceReportResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(voiceReportService.get(id));
    }

    // 보이스리포트 목록 조회
    @GetMapping
    public ResponseEntity<Page<VoiceReportResponse>> list(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                voiceReportService.list(userId, PageRequest.of(page, size))
        );
    }

    // 보이스리포트 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) throws IOException {
        voiceReportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
