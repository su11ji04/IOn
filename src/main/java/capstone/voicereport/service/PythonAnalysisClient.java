// capstone/voicereport/service/PythonAnalysisClient.java
package capstone.voicereport.service;

import capstone.voicereport.dto.AnalysisReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class PythonAnalysisClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(10);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // ✅ 생성자에 @Qualifier로 주입
    public PythonAnalysisClient(
            @Qualifier("pythonAnalyzerWebClient") WebClient webClient,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public AnalysisReportDto analyze(
            byte[] audioBytes,
            String filename,
            String userIdOrNull,
            Map<String, Object> userProfileMap
    ) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();

        String safeName = (filename != null && !filename.isBlank()) ? filename : "audio.wav";
        body.part("audio", new ByteArrayResource(audioBytes) {
                    @Override public String getFilename() { return safeName; }
                })
                .filename(safeName)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        if (userIdOrNull != null && !userIdOrNull.isBlank()) {
            body.part("user_id", userIdOrNull)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        }

        if (userProfileMap != null && !userProfileMap.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(userProfileMap);
                log.info("[PY-SEND] user_profile_json length(bytes UTF-8)={}", json.getBytes(StandardCharsets.UTF_8).length);
                log.info("[PY-SEND] user_profile_json preview={}", json.substring(0, Math.min(200, json.length())));
                body.part("user_profile_json", json)
                        .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
            } catch (Exception e) {
                log.warn("Failed to serialize userProfileMap: {}", e.toString());
            }
        } else {
            log.warn("[PY-SEND] user_profile_map is empty -> NOT sending user_profile_json");
        }

        try {
            String bodyStr = webClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromMultipartData(body.build()))
                    .exchangeToMono(res -> res.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(s -> {
                                HttpStatusCode code = res.statusCode(); // ✅ 타입 교정
                                log.info("[STEP4][PY-RECV] status={}, body.len={}, preview={}",
                                        code.value(), s.length(),
                                        s.substring(0, Math.min(200, s.length())));
                                if (!code.is2xxSuccessful()) {
                                    throw new RuntimeException("Python HTTP " + code.value() + " body: " + s);
                                }
                                return s;
                            })
                    )
                    .timeout(REQUEST_TIMEOUT)
                    .doOnError(err -> log.error("[STEP4][PY-ERR] {}", err.toString(), err))
                    .block();

            if (bodyStr == null || bodyStr.isBlank()) {
                log.warn("[STEP4][PY-RECV] empty body from Python");
                return null;
            }

            AnalysisReportDto ar = objectMapper.readValue(bodyStr, AnalysisReportDto.class);

            if (ar != null) {
                log.info("[STEP4][SVC] Python 응답 매핑 OK: subTitle={}, lenSeconds={}, summary?={}, freq?={}, expr?={}, timelineLen={}",
                        ar.getSubTitle(),
                        ar.getLength(),
                        ar.getConversationSummary() != null,
                        ar.getFrequency() != null,
                        ar.getExpression() != null,
                        (ar.getEmotion() != null && ar.getEmotion().getTimeline() != null)
                                ? ar.getEmotion().getTimeline().size() : 0
                );
            } else {
                log.warn("[STEP4][SVC] parsed AnalysisReportDto is null");
            }
            return ar;

        } catch (Exception e) {
            log.error("[STEP4][PY->SVC] parse or call failed: {}", e.toString(), e);
            return null;
        }
    }
}
