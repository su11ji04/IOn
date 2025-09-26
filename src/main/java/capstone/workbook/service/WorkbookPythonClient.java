package capstone.workbook.service;

import capstone.workbook.dto.SimNextResponse;
import capstone.workbook.dto.WorkbookFeedbackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class WorkbookPythonClient {

    private final WebClient webClient;

    public WorkbookPythonClient(@Qualifier("workbookWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /** 기존: 워크북 시뮬레이션(액티비티) 생성 */
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

    /** 시뮬레이션 다음 턴 (부모 발화 → 아이 반응) */
    public SimNextResponse simNext(Map<String, Object> payload) {
        log.info("[WORKBOOK] simNext payload keys={}", payload.keySet());
        return webClient.post()
                .uri("/workbook/sim/next")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(SimNextResponse.class)
                .block();
    }

    /** ✅ 최종 종합 피드백 생성 (FastAPI /workbook/feedback) */
    public WorkbookFeedbackResponse finalFeedback(Map<String, Object> payload) {
        log.info("[WORKBOOK] finalFeedback payload keys={}", payload.keySet());
        return webClient.post()
                .uri("/workbook/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(WorkbookFeedbackResponse.class)
                .block();
    }
}
