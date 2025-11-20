package capstone.voicereport.service;

import capstone.voicereport.dto.VoiceReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class PythonAnalysisClient {

    private final @Qualifier("pythonAnalyzerWebClient") WebClient pythonAnalyzerWebClient;

    public VoiceReportResponse analyze(byte[] wavBytes, int userId) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        ByteArrayResource fileRes = new ByteArrayResource(wavBytes) {
            @Override
            public String getFilename() {
                return "audio.wav";
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType("audio/wav"));

        form.add("audio", new HttpEntity<>(fileRes, fileHeaders));
        form.add("reportId", 100);

        try {
            return pythonAnalyzerWebClient.post()
                    .uri("/api/voice-report")
                    .header("userId", String.valueOf(userId))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromMultipartData(form))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp ->
                            resp.bodyToMono(String.class)
                                    .map(body -> new PythonBadRequestException(
                                            "[4xx] Python analyzer responded: " + body))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, resp ->
                            resp.bodyToMono(String.class)
                                    .map(body -> new PythonServerException(
                                            "[5xx] Python analyzer error: " + body))
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
        public PythonBadRequestException(String msg) {
            super(msg);
        }
    }

    public static class PythonServerException extends RuntimeException {
        public PythonServerException(String msg) {
            super(msg);
        }
    }
}