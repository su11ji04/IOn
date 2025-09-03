// src/main/java/capstone/workbook/service/WorkbookPythonClient.java
package capstone.workbook.service;

import capstone.workbook.dto.WorkbookSimulateRequest;
import capstone.workbook.dto.WorkbookSimulateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class WorkbookPythonClient {

    private final WebClient webClient;

    // ★ 명시적으로 workbookWebClient를 주입
    public WorkbookPythonClient(@Qualifier("workbookWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public WorkbookSimulateResponse simulate(WorkbookSimulateRequest req) {
        log.info("[WORKBOOK] simulate topic={}, userId={}", req.getTopic(), req.getUserId());
        return webClient.post()
                .uri("/workbook/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(WorkbookSimulateResponse.class)
                .block();
    }
}
