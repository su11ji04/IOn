package capstone.voicereport.service;

import capstone.voicereport.dto.VoiceReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PythonAnalysisClient {

    @Value("${analysis.python.base-url}")
    private String baseUrl;

    public VoiceReportResponse analyze(byte[] wavBytes,String userId) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        ByteArrayResource fileRes = new ByteArrayResource(wavBytes) {
            @Override public String getFilename() { return "audio.wav"; }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType("audio/wav"));
        form.add("audio", new HttpEntity<>(fileRes, fileHeaders));
        form.add("userId", userId);

        try {
            return WebClient.builder()
                    .baseUrl(baseUrl)
                    .build()
                    .post()
                    .uri("/voice-report/from-audio")
                    .header("X-User-Id", userId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(form))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new PythonBadRequestException(body)))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new PythonServerException(body)))
                    )
                    .bodyToMono(VoiceReportResponse.class)
                    .block();
        } catch (PythonBadRequestException | PythonServerException e) {
            throw e;
        } catch (Exception e) {
            throw new PythonServerException("Python call failed: " + e.getMessage());
        }
    }

    public static class PythonBadRequestException extends RuntimeException {
        public PythonBadRequestException(String msg) { super(msg); }
    }
    public static class PythonServerException extends RuntimeException {
        public PythonServerException(String msg) { super(msg); }
    }
}
