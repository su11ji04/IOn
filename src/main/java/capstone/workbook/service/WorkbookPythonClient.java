package capstone.workbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
public class WorkbookPythonClient {

    private final @Qualifier("workbookWebClient") WebClient workbookWebClient;

    public WorkbookPythonClient(@Qualifier("workbookWebClient") WebClient workbookWebClient) {
        this.workbookWebClient = workbookWebClient;
    }

    public Map<String, Object> createActivity(String userId) {
        try {
            return workbookWebClient.post()
                    .uri("/workbook/sequence/create")
                    .header("X-User-Id", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("userId", userId))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new PythonBadRequestException(body)))
                    )
                    .onStatus(HttpStatusCode::is5xxServerError, resp ->
                            resp.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new PythonServerException(body)))
                    )
                    .bodyToMono(GenericMap.class)
                    .map(GenericMap::get)
                    .block();
        } catch (PythonBadRequestException | PythonServerException e) {
            throw e;
        } catch (Exception e) {
            throw new PythonServerException("Python call failed: " + e.getMessage());
        }
    }

    static class GenericMap extends java.util.HashMap<String, Object> {
        Map<String, Object> get() { return this; }
    }

    public static class PythonBadRequestException extends RuntimeException {
        public PythonBadRequestException(String msg) { super(msg); }
    }
    public static class PythonServerException extends RuntimeException {
        public PythonServerException(String msg) { super(msg); }
    }
}
