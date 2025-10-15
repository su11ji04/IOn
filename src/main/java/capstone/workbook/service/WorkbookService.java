package capstone.workbook.service;

import capstone.workbook.dto.*;
import capstone.workbook.entity.Workbook;
import capstone.workbook.error.WorkbookException;
import capstone.workbook.repository.WorkbookRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkbookService {

    private final WorkbookPythonClient py;
    private final WorkbookRepository wbRepo;
    private final ObjectMapper om;

    // 워크북 생성 및 저장
    @Transactional
    public Long createAndSave(String userId, int chapterId, Long workbookId) {
        validateChapterRange(chapterId, chapterId);
        String chapterCode = toChapterCode(chapterId);

        Map<String, Object> createOut = py.createActivity(userId, chapterCode, workbookId);
        if (createOut == null) {
            throw WorkbookException.generationFailed("Python create returned null");
        }

        int activityCount = ((List<?>) createOut.getOrDefault("activities", List.of())).size();
        if (activityCount <= 0) {
            throw WorkbookException.schemaInvalid("activities must not be empty");
        }
        String activityTitle = String.valueOf(createOut.getOrDefault("activity_title", "워크북"));

        Workbook wb = Workbook.builder()
                .chapterId(chapterId)
                .userId(userId)
                .activityTitle(activityTitle)
                .activityCount(activityCount)
                .activityJson(writeJson(createOut))
                .finished(false)
                .build();

        wbRepo.save(wb);
        return wb.getId();
    }

    private static String toChapterCode(int n) {
        return "chapter_" + n;
    }

    private void validateChapterRange(int n, Object rawValueForMsg) {
        if (n < 1 || n > 5) {
            throw WorkbookException.schemaInvalid("chapter_id out of range (1..5): " + rawValueForMsg);
        }
    }

    //워크북 실행
    @Transactional(readOnly = true)
    public WorkbookRunResponse buildRunPayload(int chapterId, Long workbookId, String userId) {
        Workbook wb = wbRepo.findById(workbookId)
                .orElseThrow(() -> WorkbookException.notFound(workbookId));

        if (!wb.getUserId().equals(userId)) {
            throw WorkbookException.forbidden(
                    "다른 사용자의 워크북 접근",
                    "userId=" + userId + ", owner=" + wb.getUserId() + ", workbookId=" + workbookId
            );
        }

        if (wb.getChapterId() != chapterId) {
            throw WorkbookException.badRequest(
                    "요청 경로의 챕터와 워크북의 챕터 불일치",
                    "path.chapterId=" + chapterId + ", wb.chapterId=" + wb.getChapterId()
            );
        }

        Map<String, Object> json = readJsonAsMap(wb.getActivityJson());

        String title = String.valueOf(json.getOrDefault("activity_title", wb.getActivityTitle()));
        List<WorkbookActivity> activities = om.convertValue(
                json.get("activities"),
                new TypeReference<List<WorkbookActivity>>() {}
        );

        return WorkbookRunResponse.builder()
                .chapterId(wb.getChapterId())
                .workbookId(workbookId)
                .activityTitle(title)
                .activities(activities)
                .build();
    }

    //워크북 답변 저장
    @Transactional
    public void saveAnswer(int chapterId, Long workbookId, String userId, WorkbookAnswerRequest req) {
        Workbook wb = wbRepo.findById(workbookId)
                .orElseThrow(() -> WorkbookException.notFound(workbookId));

        if (!wb.getUserId().equals(userId)) {
            throw WorkbookException.forbidden(
                    "다른 사용자의 워크북 저장",
                    "userId=" + userId + ", owner=" + wb.getUserId() + ", workbookId=" + workbookId
            );
        }

        if (wb.getChapterId() != chapterId) {
            throw WorkbookException.badRequest(
                    "요청 경로의 챕터와 워크북의 챕터 불일치",
                    "path.chapterId=" + chapterId + ", wb.chapterId=" + wb.getChapterId()
            );
        }

        wb.setMcqAnswer(req.getOptionAnswer());
        wb.setWritingAnswer(req.getWritingAnswer());
        wb.setSimAnswer(req.getSimAnswer());
        wb.setFinished(Boolean.TRUE.equals(req.getFinished()));

        wbRepo.save(wb);
    }

    private String nullSafeTrim(String s) {
        return (s == null) ? null : s.trim();
    }

    //피드백 생성 및 반환
    @Transactional
    public WorkbookFeedbackDto generateAndSaveFeedback(Long workbookId, String userId) {
        Workbook wb = wbRepo.findById(workbookId)
                .orElseThrow(() -> WorkbookException.notFound(workbookId));

        if (!wb.getUserId().equals(userId)) {
            throw WorkbookException.forbidden(
                    "다른 사용자의 워크북 피드백",
                    "userId=" + userId + ", owner=" + wb.getUserId() + ", workbookId=" + workbookId
            );
        }

        Map<String, Object> payload = buildFeedbackPayload(wb);
        Map<String, Object> pyOut;
        try {
            pyOut = py.generateFeedback(userId, payload);
        } catch (Exception e) {

            throw WorkbookException.generationFailed("python-call: " + e.getMessage());
        }

        if (pyOut == null || pyOut.isEmpty()) {
            throw WorkbookException.generationFailed("python-response empty");
        }

        Object raw = pyOut.get("workbook_feedback");
        String feedback = (raw == null) ? "" : String.valueOf(raw).trim();

        if (feedback.isEmpty()) {
            throw WorkbookException.generationFailed("workbook_feedback is empty");

        }

        wb.setFeedback(feedback);
        wb.setFinished(true);
        wbRepo.save(wb);


        return WorkbookFeedbackDto.builder()
                .workbookFeedback(wb.getFeedback())
                .build();
    }

   // 피드백 payload
   private Map<String, Object> buildFeedbackPayload(Workbook wb) {
       Map<String, Object> json = readJsonAsMap(wb.getActivityJson());

       String chapterCode  = "chapter_" + wb.getChapterId();
       String workbookId  = String.valueOf(wb.getId());

       String optionAnswer  = nvl(wb.getMcqAnswer());
       String writingAnswer = nvl(wb.getWritingAnswer());
       String simAnswer     = nvl(wb.getSimAnswer());

       String optimalOption  = findOptimal(json, new String[]{"mcq","선택형"},     "optimal_option");
       String optimalWriting = findOptimal(json, new String[]{"writing","작성형"}, "optimal_writing");
       String optimalSim     = findOptimal(json, new String[]{"sim","시뮬레이션"}, "optimal_sim");

       return Map.of(
               "chapter_id",      chapterCode,
               "workbook_id",     workbookId,
               "optionAnswer",    optionAnswer,
               "optimal_option",  optimalOption,
               "writingAnswer",   writingAnswer,
               "optimal_writing", optimalWriting,
               "simAnswer",       simAnswer,
               "optimal_sim",     optimalSim
       );
   }

    private String nvl(String s) { return (s == null) ? "" : s; }

    @SuppressWarnings("unchecked")
    private String findOptimal(Map<String, Object> json, String[] expectTypes, String targetKey) {
        Object acts = json.get("activities");
        if (!(acts instanceof List<?> list)) return "";
        for (Object o : list) {
            if (o instanceof Map<?,?> m) {
                Object type = m.get("type");
                if (type != null) {
                    String t = String.valueOf(type);
                    for (String exp : expectTypes) {
                        if (t.equalsIgnoreCase(exp)) {
                            // 기본 시도: targetKey
                            Object v = m.get(targetKey);
                            if (v != null) return String.valueOf(v);

                            // ★ 폴백: 작성형/시뮬레이션 전용 대체 키
                            if (("writing".equalsIgnoreCase(exp) || "작성형".equalsIgnoreCase(exp))) {
                                Object ex = m.get("example_answer");           // 작성형 폴백
                                if (ex != null) return String.valueOf(ex);
                            }
                            if (("sim".equalsIgnoreCase(exp) || "시뮬레이션".equalsIgnoreCase(exp))) {
                                Object ex = m.get("ai_optimal_response");      // 시뮬 폴백
                                if (ex != null) return String.valueOf(ex);
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    //워크북 챕터별 목록
    @Transactional(readOnly = true)
    public Page<WorkbookListResponse> list(String userId, int chapterId, Pageable pageable) {
        return wbRepo.findByUserIdAndChapterIdOrderByCreatedAtDesc(userId, chapterId, pageable);
    }

    private String writeJson(Object obj) {
        try {
            return om.writeValueAsString(obj);
        } catch (Exception e) {
            throw WorkbookException.schemaInvalid("Failed to serialize activity json");
        }
    }

    private Map<String, Object> readJsonAsMap(String json) {
        try {
            return om.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw WorkbookException.schemaInvalid("invalid activity json");
        }
    }
}
