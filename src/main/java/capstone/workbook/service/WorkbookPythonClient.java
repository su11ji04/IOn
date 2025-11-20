package capstone.workbook.service;

import capstone.workbook.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkbookPythonClient {

    // 9096: 워크북/시뮬레이션 "생성" 서버
    @Qualifier("workbookCreateWebClient")
    private final WebClient workbookCreateWebClient;

    // 9098: 워크북/시뮬레이션 "피드백" 서버
    @Qualifier("workbookFeedbackWebClient")
    private final WebClient workbookFeedbackWebClient;


    // 회원 가입 -> 워크북 생성 (9096)
    public WorkbookDto createWorkbook(int userId, int chapterId, int lessonId) {

        Map<String, Object> body = new HashMap<>();
        body.put("chapterId", chapterId);
        body.put("lessonId", lessonId);

        // 🔹 내가 보내는 요청 로그
        log.info("[WORKBOOK-REQ] POST /api/workbooks header.user_id={} body={}", userId, body);

        return workbookCreateWebClient.post()
                .uri("/api/workbooks")
                .header("userId", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(WorkbookDto.class)
                .doOnNext(res ->
                        log.info("[WORKBOOK-RES] /api/workbooks user_id={} response={}", userId, res)
                )
                .block();
    }

    // 워크북 피드백 생성 (9098)
    public String createWorkbookFeedback(int userId, WorkbookDto dto) {

        log.info("[WORKBOOK-FEEDBACK-REQ] POST /api/workbook_feedback header.user_id={} body={}", userId, dto);

        WorkbookFeedbackDto response = workbookFeedbackWebClient.post()
                .uri("/api/workbook_feedback")
                .header("userId", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(WorkbookFeedbackDto.class)
                .doOnNext(res ->
                        log.info("[WORKBOOK-FEEDBACK-RES] /api/workbook_feedback user_id={} response={}", userId, res)
                )
                .block();

        if (response == null) {
            throw new IllegalStateException("워크북 피드백 응답이 비어 있습니다.");
        }

        return response.getWorkbookFeedback();
    }


    // 회원 가입 -> 시뮬레이션 생성 (9096)
    public SimulationPythonResponseDto createSimulation(
            int userId, int chapterId, int lessonId, List<DialogueDto> dialogues) {

        Map<String, Object> body = new HashMap<>();
        body.put("chapterId", chapterId);
        body.put("lessonId", lessonId);
        body.put("dialogues", dialogues);

        log.info("[SIMULATION-REQ] POST /api/workbook_simulation header.user_id={} body={}", userId, body);

        return workbookCreateWebClient.post()
                .uri("/api/workbook_simulation")
                .header("userId", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SimulationPythonResponseDto.class)
                .doOnNext(res ->
                        log.info("[SIMULATION-RES] /api/workbook_simulation user_id={} response={}", userId, res)
                )
                .block();
    }

    // SIMULATION 피드백 생성 (9098)
    public String createSimulationFeedback(int userId, List<DialogueDto> dialogues) {

        Map<String, Object> body = new HashMap<>();
        body.put("dialogues", dialogues);

        try {
            ObjectMapper mapper = new ObjectMapper();
            log.info("[SIM-FEEDBACK-REQ-BODY-JSON] {}", mapper.writeValueAsString(body));
        } catch (Exception e) {
            log.warn("JSON log failed", e);
        }

        SimulationFeedbackDto response = workbookFeedbackWebClient.post()
                .uri("/api/workbook_feedback_simulation")
                .header("userId", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(SimulationFeedbackDto.class)
                .doOnNext(res ->
                        log.info("[SIMULATION-FEEDBACK-RES] /api/workbook_feedback_simulation user_id={} response={}", userId, res)
                )
                .block();

        if (response == null) {
            throw new IllegalStateException("시뮬레이션 피드백 응답이 비어 있습니다.");
        }
        return response.getSimulationFeedback();
    }
}
