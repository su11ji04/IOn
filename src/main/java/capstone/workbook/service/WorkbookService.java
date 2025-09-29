package capstone.workbook.service;

import capstone.workbook.dto.CreateWorkbookRequest;
import capstone.workbook.dto.ListItemDto;
import capstone.workbook.dto.ListResponse;
import capstone.workbook.dto.WorkbookDetailResponse;
import capstone.workbook.dto.RunDtos.StartResponse;
import capstone.workbook.entity.Workbook;
import capstone.workbook.entity.WorkbookRun;
import capstone.workbook.repository.WorkbookRepository;
import capstone.workbook.repository.WorkbookRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WorkbookService {

    private final WorkbookPythonClient py;
    private final WorkbookRepository wbRepo;
    private final WorkbookRunRepository runRepo;
    private final ObjectMapper om;

    public WorkbookService(WorkbookPythonClient py,
                           WorkbookRepository wbRepo,
                           WorkbookRunRepository runRepo,
                           ObjectMapper om) {
        this.py = py;
        this.wbRepo = wbRepo;
        this.runRepo = runRepo;
        this.om = om;
    }

    // 워크북 생성 + 저장
    public Long createAndSave(CreateWorkbookRequest req) {
        final String fixedUserId = "u001"; // [개발용] USER ID
        final String fixedTopic  = "떼쓰는 아이"; // [개발용] USER ID
        log.info("[WORKBOOK SERVICE] using fixed userId={}, topic={}", fixedUserId, fixedTopic);

        @SuppressWarnings("unchecked")
        Map<String, Object> user = req.getUser() == null
                ? new java.util.HashMap<>()
                : new java.util.HashMap<>(req.getUser());
        user.put("user_id", fixedUserId);

        var pyReq = Map.of("topic", fixedTopic, "user", user);


        // 워크북 생성
        Map<String, Object> createOut = py.createActivity(pyReq).block();
        if (createOut == null) {
            throw new IllegalStateException("Python create returned null");
        }

        // 워크북 내용 저장
        Object activitiesObj = createOut.get("activities");
        int activityCount = (activitiesObj instanceof List)
                ? ((List<?>) activitiesObj).size()
                : (activitiesObj == null ? 0 : 1);

        Workbook wb = new Workbook();
        wb.setUserId(fixedUserId);
        wb.setTopic(fixedTopic);

        try {
            wb.setActivityJson(om.writeValueAsString(createOut));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize activity json", e);
        }
        wb.setActivityCount(activityCount);
        wbRepo.save(wb);

        return wb.getId();
    }

    // 워크북 리스트 조회
    public ListResponse listSimple(String userId, Pageable pageable) {
        Page<Workbook> page = (userId == null || userId.isBlank())
                ? wbRepo.findAll(pageable)
                : wbRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<ListItemDto> items = page.getContent().stream()
                .map(wb -> ListItemDto.builder()
                        .id(wb.getId())
                        .topic(wb.getTopic())
                        .activityCount(wb.getActivityCount())
                        .createdAt(wb.getCreatedAt())
                        .build())
                .toList();

        return ListResponse.builder()
                .userId((userId == null || userId.isBlank()) ? null : userId)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .items(items)
                .build();
    }

    // [개발용] 워크북 단건 조회
    public WorkbookDetailResponse getOne(Long id) throws IOException {
        Workbook wb = wbRepo.findById(id).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<String,Object> activity = om.readValue(wb.getActivityJson(), Map.class);

        return WorkbookDetailResponse.builder()
                .id(wb.getId())
                .userId(wb.getUserId())
                .topic(wb.getTopic())
                .activityCount(wb.getActivityCount())
                .createdAt(wb.getCreatedAt())
                .activity(activity)
                .build();
    }


    // MCQ 질문
    public StartResponse runStart(Long workbookId, String userId) throws Exception {
        Workbook wb = wbRepo.findById(workbookId).orElseThrow();
        Map activity = om.readValue(wb.getActivityJson(), Map.class);
        Map<String,Object> mcq = pick(activity, "MCQ");

        WorkbookRun run = WorkbookRun.builder()
                .workbookId(workbookId)
                .userId(userId)
                .step("MCQ")
                .build();
        runRepo.save(run);

        StartResponse out = new StartResponse();
        out.setRunId(run.getId());
        out.setMcq(mcq);
        return out;
    }

    // MCQ 대답
    public Map<String,Object> answerMcq(Long runId, String answer) throws Exception {
        WorkbookRun run = runRepo.findById(runId).orElseThrow();
        ensureStep(run, "MCQ");

        run.setMcqAnswer(answer);
        run.setStep("WRITING");
        runRepo.save(run);

        Workbook wb = wbRepo.findById(run.getWorkbookId()).orElseThrow();
        Map activity = om.readValue(wb.getActivityJson(), Map.class);
        return pick(activity, "WRITING");
    }

    // WRITING 대답
    public Map<String,Object> answerWriting(Long runId, String text) throws Exception {
        WorkbookRun run = runRepo.findById(runId).orElseThrow();
        ensureStep(run, "WRITING");
        run.setWritingText(text);

        Workbook wb = wbRepo.findById(run.getWorkbookId()).orElseThrow();
        Map activity = om.readValue(wb.getActivityJson(), Map.class);
        Map<String,Object> sim = pick(activity, "SIMULATION");

        List<Map<String,String>> hist = new ArrayList<>();
        hist.add(Map.of("role","ai","text", String.valueOf(sim.getOrDefault("ai_first_line",""))));
        run.setSimHistoryJson(om.writeValueAsString(hist));
        run.setStep("SIM1");
        runRepo.save(run);

        return sim;
    }

    // SIM 질문_2
    public Map<String,Object> simNext(Long runId, String parentReply) throws Exception {
        WorkbookRun run = runRepo.findById(runId).orElseThrow();
        ensureStep(run, "SIM1", "SIM2");

        Workbook wb = wbRepo.findById(run.getWorkbookId()).orElseThrow();
        Map activity = om.readValue(wb.getActivityJson(), Map.class);
        String topic = String.valueOf(activity.getOrDefault("activity_title", wb.getTopic()));
        Map<String,Object> sim = pick(activity, "SIMULATION");
        String situation = String.valueOf(sim.getOrDefault("situation",""));

        // SIM HISTORY
        List<Map<String,String>> hist = parseHist(run.getSimHistoryJson());

        // Python 요청 바디
        Map<String,Object> req = Map.of(
                "topic", topic,
                "situation", situation,
                "history", hist,
                "parent_reply", parentReply == null ? "" : parentReply
        );

        // SIM 질문_2: PYTHON 호출
        Map<String,Object> pyOut = py.simNext(req).block();
        String aiLine = String.valueOf(pyOut.getOrDefault("ai_line",""));
        boolean finished = Boolean.TRUE.equals(pyOut.get("finished"));

        // SIM HISTORY 추가
        if (parentReply != null && !parentReply.isBlank()) {
            hist.add(Map.of("role","user","text", parentReply));
        }
        if (aiLine != null && !aiLine.isBlank()) {
            hist.add(Map.of("role","ai","text", aiLine));
        }

        run.setSimHistoryJson(om.writeValueAsString(hist));
        run.setStep(finished ? "FEEDBACK" : nextSimStep(run.getStep()));
        runRepo.save(run);

        return Map.of("aiLine", aiLine, "finished", finished);
    }

    // 워크북 활동 피드백
    public Map<String,Object> finalizeFeedback(Long runId) throws Exception {
        WorkbookRun run = runRepo.findById(runId).orElseThrow();
        ensureStep(run, "FEEDBACK");

        Workbook wb = wbRepo.findById(run.getWorkbookId()).orElseThrow();
        Map activity = om.readValue(wb.getActivityJson(), Map.class);

        Map<String,Object> mcq = pick(activity, "MCQ");
        Map<String,Object> writing = pick(activity, "WRITING");
        List<Map<String,String>> hist = parseHist(run.getSimHistoryJson());

        Map<String,Object> req = Map.of(
                "topic", String.valueOf(activity.getOrDefault("activity_title", wb.getTopic())),
                "mcq", List.of(Map.of(
                        "instruction", mcq.get("instruction"),
                        "user_answer", run.getMcqAnswer(),
                        "optimal_option", mcq.get("optimal_option")
                )),
                "writing", List.of(Map.of(
                        "instruction", writing.get("instruction"),
                        "user_text", run.getWritingText()
                )),
                "sim_history", hist
        );

        Map<String,Object> fb = py.feedback(req).block();
        run.setFinalFeedbackJson(om.writeValueAsString(fb));
        run.setFinished(true);
        runRepo.save(run);
        return fb;
    }

    // 단계 저장
    @SuppressWarnings("unchecked")
    private Map<String,Object> pick(Map activity, String type){
        for (Object o : (List<?>)activity.getOrDefault("activities", List.of())) {
            Map<String,Object> m = (Map<String,Object>) o;
            if (type.equals(m.get("type"))) return m;
        }
        throw new IllegalStateException(type + " not found");
    }

    // 단계 확인
    private void ensureStep(WorkbookRun run, String... allowed){
        for (String s: allowed) if (s.equals(run.getStep())) return;
        throw new IllegalStateException("Invalid step: " + run.getStep());
    }

    // SIM 2번째 턴
    private String nextSimStep(String now){
        return "SIM1".equals(now) ? "SIM2" : "SIM2";
    }


    // JSON -> LIST
    @SuppressWarnings("unchecked")
    private List<Map<String,String>> parseHist(String json) throws Exception {
        if (json==null || json.isBlank()) return new ArrayList<>();
        return om.readValue(json, List.class);
    }
}
