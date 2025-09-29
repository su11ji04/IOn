// src/main/java/capstone/workbook/service/WorkbookPythonClient.java
package capstone.workbook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
public class WorkbookPythonClient {

    private final WebClient workbookWebClient;

    public WorkbookPythonClient(@Qualifier("workbookWebClient") WebClient workbookWebClient) {
        this.workbookWebClient = workbookWebClient;
    }

    public Mono<Map<String, Object>> createActivity(Map<String, Object> body) {
        return workbookWebClient.post()
                .uri("/workbook/sequence/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GenericMap.class)
                .map(GenericMap::get);
    }

    public Mono<Map<String, Object>> sequenceStart(Map<String, Object> body) {
        return workbookWebClient.post()
                .uri("/workbook/sequence/start")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GenericMap.class)
                .map(GenericMap::get);
    }

    static class GenericMap extends java.util.HashMap<String, Object> {
        Map<String, Object> get() { return this; }
    }

    public Mono<Map<String, Object>> simNext(Map<String, Object> body) {
        return workbookWebClient.post()
                .uri("/workbook/sequence/sim/next")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GenericMap.class)
                .map(GenericMap::get);
    }

    public Mono<Map<String, Object>> feedback(Map<String, Object> body) {
        return workbookWebClient.post()
                .uri("/workbook/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GenericMap.class)
                .map(GenericMap::get);
    }
}
