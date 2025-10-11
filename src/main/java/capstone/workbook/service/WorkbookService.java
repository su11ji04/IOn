package capstone.workbook.service;

import capstone.workbook.dto.RunDtos.StartResponse;
import capstone.workbook.dto.WorkbookListResponse;
import capstone.workbook.entity.Workbook;
import capstone.workbook.entity.WorkbookRun;
import capstone.workbook.error.WorkbookException;
import capstone.workbook.repository.WorkbookRepository;
import capstone.workbook.repository.WorkbookRunRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkbookService {

    private final WorkbookPythonClient py;
    private final WorkbookRepository wbRepo;
    private final WorkbookRunRepository runRepo;
    private final ObjectMapper om;

    public Long createAndSave(String userId) {
        Map<String, Object> createOut;
        try {
            createOut = py.createActivity(userId);
        } catch (Exception e) {
            throw WorkbookException.generationFailed("Python createActivity failed: " + e.getMessage());
        }

        if (createOut == null) {
            throw WorkbookException.generationFailed("Python create returned null");
        }


        // activity_count
        Object activitiesObj = createOut.get("activities");
        int activityCount = (activitiesObj instanceof List)
                ? ((List<?>) activitiesObj).size()
                : (activitiesObj == null ? 0 : 1);

        if (activityCount <= 0) {
            throw WorkbookException.schemaInvalid("activities must not be empty");
        }

        // activity_title
        String activityTitle = String.valueOf(createOut.getOrDefault("activity_title", "워크북"));

        Workbook wb = Workbook.builder()
                .userId(userId)
                .activityTitle(activityTitle)
                .activityCount(activityCount)
                .activityJson(writeJson(createOut))
                .build();

        wbRepo.save(wb);
        return wb.getId();
    }

    public StartResponse runStart(Long workbookId, String userId) throws Exception {
        Workbook wb = wbRepo.findById(workbookId)
                .orElseThrow(() -> WorkbookException.notFound(workbookId));

        Map activity;
        try {
            activity = om.readValue(wb.getActivityJson(), Map.class);
        } catch (Exception e) {
            throw WorkbookException.schemaInvalid("invalid activity json");
        }
        Map<String,Object> mcq = pick(activity, "선택형");

        WorkbookRun run = WorkbookRun.builder()
                .workbookId(workbookId)
                .userId(userId)
                .step("MCQ")
                .finished(false)
                .build();
        runRepo.save(run);

        return StartResponse.builder()
                .runId(run.getId())
                .mcq(mcq)
                .build();
    }

    public Map<String,Object> answerMcq(Long runId, String answer) throws Exception {
        WorkbookRun run = runRepo.findById(runId)
                .orElseThrow(() -> WorkbookException.notFound(runId));

        ensureStep(run, "MCQ");

        run.setMcqAnswer(answer);
        run.setStep("WRITING");
        runRepo.save(run);

        Workbook wb = wbRepo.findById(run.getWorkbookId())
                .orElseThrow(() -> WorkbookException.notFound(run.getWorkbookId()));

        Map activity;
        try {
            activity = om.readValue(wb.getActivityJson(), Map.class);
        } catch (Exception e) {
            throw WorkbookException.schemaInvalid("invalid activity json");
        }
        return pick(activity, "작성형");
    }

    public Map<String,Object> answerWriting(Long runId, String text) throws Exception {
        WorkbookRun run = runRepo.findById(runId)
                .orElseThrow(() -> WorkbookException.notFound(runId));

        ensureStep(run, "WRITING");

        run.setWritingText(text);
        run.setStep("SIM");
        runRepo.save(run);

        Workbook wb = wbRepo.findById(run.getWorkbookId())
                .orElseThrow(() -> WorkbookException.notFound(run.getWorkbookId()));

        Map activity;
        try {
            activity = om.readValue(wb.getActivityJson(), Map.class);
        } catch (Exception e) {
            throw WorkbookException.schemaInvalid("invalid activity json");
        }
        Map<String,Object> sim = pick(activity, "시뮬레이션");

        return sim;
    }

    @Transactional(readOnly = true)
    public Page<WorkbookListResponse> list(String userId, Pageable pageable) {
        return wbRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> pick(Map activity, String type){
        for (Object o : (List<?>)activity.getOrDefault("activities", List.of())) {
            Map<String,Object> m = (Map<String,Object>) o;
            if (type.equals(m.get("type"))) return m;
        }
        throw WorkbookException.schemaInvalid(type + " not found");
    }

    private void ensureStep(WorkbookRun run, String... allowed){
        for (String s: allowed) if (s.equals(run.getStep())) return;
        throw WorkbookException.conflictState("Invalid step: " + run.getStep());
    }

    private String writeJson(Object obj){
        try {
            return om.writeValueAsString(obj);
        } catch (Exception e) {
            throw WorkbookException.schemaInvalid("Failed to serialize activity json");
        }
    }
}
