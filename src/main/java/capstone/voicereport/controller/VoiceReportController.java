package capstone.voicereport.controller;

import capstone.voicereport.dto.CreateVoiceReportRequest;
import capstone.voicereport.dto.VoiceReportResponse;
import capstone.voicereport.service.VoiceReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/voice-reports")
public class VoiceReportController {

    private final VoiceReportService voiceReportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VoiceReportResponse> create(
            @RequestPart("audio") MultipartFile audio
    ) throws IOException {

        // [STEP1] 컨트롤러가 오디오를 받았는지 1차 확인
        log.info("[STEP1][CTRL] received audio: name={}, size={}, contentType={}",
                audio.getOriginalFilename(), audio.getSize(), audio.getContentType());

        VoiceReportResponse res = voiceReportService.createWithFixedUser(audio);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }



    @GetMapping("/{id}")
    public ResponseEntity<VoiceReportResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(voiceReportService.get(id));
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) throws IOException {
        voiceReportService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audio")
    public ResponseEntity<InputStreamResource> downloadAudio(@PathVariable("id") Long id) throws IOException {
        Path p = voiceReportService.getAudioPath(id);
        String ct = Files.probeContentType(p);
        InputStreamResource resource = new InputStreamResource(Files.newInputStream(p));
        return ResponseEntity.ok()
                // WAV만 취급이라면 아래를 MediaType.valueOf("audio/wav")로 고정해도 됩니다.
                .contentType(ct != null ? MediaType.parseMediaType(ct) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + p.getFileName() + "\"")
                .contentLength(Files.size(p))
                .body(resource);
    }
}
