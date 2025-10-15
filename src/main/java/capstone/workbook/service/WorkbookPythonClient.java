package capstone.workbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkbookPythonClient {

    private final @Qualifier("workbookCreateWebClient") WebClient createClient;
    private final @Qualifier("workbookFeedbackWebClient") WebClient feedbackClient;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    public Map<String, Object> createActivity(String userId, String chapterCode, Long workbookId) {
        try {
            // 권장: user_id를 바디에도 포함 (헤더 병행)
            Map<String, Object> body = (workbookId == null)
                    ? Map.of("user_id", userId, "chapter_id", chapterCode)
                    : Map.of("user_id", userId, "chapter_id", chapterCode, "workbook_id", String.valueOf(workbookId));

            return createClient.post()
                    // 너의 파이썬 엔드포인트에 맞춰 경로 확인: /api/workbooks 또는 /api/workbook/create
                    .uri("/api/workbooks")
                    .header("user_id", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(msg -> Mono.error(new PythonBadRequestException(msg))))
                    .onStatus(HttpStatusCode::is5xxServerError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(msg -> Mono.error(new PythonServerException(msg))))
                    .bodyToMono(MAP_TYPE)
                    .block();

        } catch (PythonBadRequestException | PythonServerException e) {
            throw e;
        } catch (Exception e) {
            throw new PythonServerException("Python create call failed: " + e.getMessage());
        }
    }

    public Map<String, Object> generateFeedback(String userId, Map<String, Object> payload) {
        try {
            return feedbackClient.post()
                    .uri("/api/workbook_feedback")
                    .header("user_id", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(msg -> Mono.error(new PythonBadRequestException(msg))))
                    .onStatus(HttpStatusCode::is5xxServerError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(msg -> Mono.error(new PythonServerException(msg))))
                    .bodyToMono(MAP_TYPE)
                    .block();

        } catch (PythonBadRequestException | PythonServerException e) {
            throw e;
        } catch (Exception e) {
            throw new PythonServerException("Python feedback call failed: " + e.getMessage());
        }
    }

    public static class PythonBadRequestException extends RuntimeException {
        public PythonBadRequestException(String msg) { super(msg); }
    }
    public static class PythonServerException extends RuntimeException {
        public PythonServerException(String msg) { super(msg); }
    }
}
