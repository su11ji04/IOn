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

    // WEB CLIENT BEAN 생성
    public PythonAnalysisClient(
            @Qualifier("pythonAnalyzerWebClient") WebClient webClient,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    // PYTHON API 호출(오디오, 오디오 파일 이름, USER ID, USER 정보)
    public AnalysisReportDto analyze(
            byte[] audioBytes,
            String filename,
            String userId,
            Map<String, Object> userProfileMap
    ) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();

        // AUDIO
        if (audioBytes == null || audioBytes.length == 0) {
            throw new IllegalArgumentException("Audio file must not be empty");
        }
        // AUDIO FILE NAME
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be null or blank");
        }
        String safeName = filename.trim();
        //AUDIO SETTING
        body.part("audio", new ByteArrayResource(audioBytes) {
                    @Override public String getFilename() { return safeName; }
                })
                .filename(safeName)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);



        // USER ID
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID must not be null or blank");
        }
        // USER SETTING
        body.part("user_id", userId)
                .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
        if (userProfileMap != null && !userProfileMap.isEmpty()) {
            try {
                String json = objectMapper.writeValueAsString(userProfileMap);
                log.info("[USER SETTING] user_profile_json length(bytes UTF-8)={}", json.getBytes(StandardCharsets.UTF_8).length);
                log.info("[USER SETTING] user_profile_json preview={}", json.substring(0, Math.min(200, json.length())));
                body.part("user_profile_json", json)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            } catch (Exception e) {
                log.warn("Failed to serialize userProfileMap: {}", e.toString());
            }
        } else {
            log.warn("[USER SETTING] user_profile_map is empty -> NOT sending user_profile_json");
        }



        //PYTHON 실행
        try {
            String bodyStr = webClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromMultipartData(body.build()))
                    .exchangeToMono(res -> res.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(s -> {
                                HttpStatusCode code = res.statusCode();
                                log.info("[PYTHON CODE] status={}, body.len={}",
                                        code.value(), s.length());
                                if (!code.is2xxSuccessful()) {
                                    throw new RuntimeException("Python HTTP " + code.value() + " body: " + s);
                                }
                                return s;
                            })
                    )
                    .timeout(REQUEST_TIMEOUT)
                    .doOnError(err -> log.error("[PYTHON CODE] {}", err.toString(), err))
                    .block();

            // PYTHON RETURN BODY = NULL or BLANK
            if (bodyStr == null || bodyStr.isBlank()) {
                log.warn("[PYTHON CODE] empty body from Python");
                return null;
            }

            AnalysisReportDto ar = objectMapper.readValue(bodyStr, AnalysisReportDto.class);

            if (ar != null) {
                // PYTHON RETURN BODY 파싱 성공
                log.info("[PYTHON CODE] 응답 매핑 OK: subTitle={}, summary?={}, freq?={}, expr?={}, timelineLen={}",
                        ar.getSubTitle(),
                        ar.getConversationSummary() != null,
                        ar.getFrequency() != null,
                        ar.getExpression() != null,
                        (ar.getEmotion() != null && ar.getEmotion().getTimeline() != null)
                                ? ar.getEmotion().getTimeline().size() : 0
                );
            } else {
                // PYTHON RETURN BODY 파싱 결과 NULL
                log.warn("[PYTHON CODE] parsed AnalysisReportDto is null");
            }
            return ar;

        } catch (Exception e) {
            log.error("[PYTHON CODE] parse or call failed: {}", e.toString(), e);
            return null;
        }
    }
}
