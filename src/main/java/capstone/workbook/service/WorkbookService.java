package capstone.workbook.service;

import capstone.workbook.dto.*;
import capstone.workbook.entity.SimulationSession;
import capstone.workbook.entity.Workbook;
import capstone.workbook.entity.WorkbookStepAnswer;
import capstone.workbook.repository.SimulationSessionRepository;
import capstone.workbook.repository.WorkbookRepository;
import capstone.workbook.repository.WorkbookStepAnswerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * WorkbookService - 최종본
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkbookService {

    // UserProfileLoader (Optional 반환 가정)
    private final capstone.support.userprofile.UserProfileLoader userProfileLoader;

    private final WorkbookPythonClient pythonClient;
    private final WorkbookRepository workbookRepo;
    private final SimulationSessionRepository sessionRepo;
    private final WorkbookStepAnswerRepository stepAnswerRepo;
    private final ObjectMapper objectMapper;

    /** 파이썬으로 액티비티 생성 후 저장(옵션) */
    public WorkbookSimulateResponse simulateAndSave(WorkbookSimulateRequest req, boolean save) {
        WorkbookSimulateResponse res = pythonClient.simulate(req);
        if (save) {
            try {
                int cnt = (res.getActivities() == null) ? 0 : res.getActivities().size();
                String json = objectMapper.writeValueAsString(res);

                Workbook entity = Workbook.builder()
                        .userId(req.getUserId())
                        .topic(req.getTopic())
                        .activityCount(cnt)
                        .rawJson(json)
                        .createdAt(LocalDateTime.now())
                        .build();
                workbookRepo.save(entity);
            } catch (Exception e) {
                log.error("simulateAndSave save error", e);
            }
        }
        return res;
    }

    // =========================
    // 컨트롤러에서 호출하는 3개 메서드
    // =========================

    /** ① 선택형/작성형 제출 */
    @Transactional
    public WorkbookSubmitResponse submit(WorkbookSubmitRequest req) {
        final String userId = (req.getUserId() == null) ? "u001" : req.getUserId();
        final Long preferredId = (req.getWorkbookId() == null) ? null : req.getWorkbookId();

        Workbook wb = getOrCreateDefaultWorkbook(userId, preferredId);
        if (wb.getRawJson() == null || wb.getRawJson().isBlank()) {
            throw new IllegalStateException("Workbook rawJson is empty");
        }

        final WorkbookSimulateResponse simRes;
        try {
            simRes = objectMapper.readValue(wb.getRawJson(), WorkbookSimulateResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse workbook.rawJson", e);
            throw new IllegalStateException("Invalid workbook JSON");
        }
        if (simRes.getActivities() == null || simRes.getActivities().isEmpty()) {
            throw new IllegalStateException("No activities in workbook");
        }

        final Integer stepIndex = req.getStepIndex();
        if (stepIndex == null || stepIndex < 0 || stepIndex >= simRes.getActivities().size()) {
            throw new IndexOutOfBoundsException("Invalid stepIndex: " + stepIndex);
        }
        final WorkbookActivity step = simRes.getActivities().get(stepIndex);

        // 현 스텝 저장 이력
        List<WorkbookStepAnswer> submitted = stepAnswerRepo.findByWorkbookIdAndStepIndex(wb.getId(), stepIndex);
        boolean mcqDone     = submitted.stream().anyMatch(a -> a.getStepType() == ActivityType.MCQ);
        boolean writingDone = submitted.stream().anyMatch(a -> a.getStepType() == ActivityType.WRITING);

        boolean hasMCQ     = step.getActivities().stream().anyMatch(a -> a.getType() == ActivityType.MCQ);
        boolean hasWriting = step.getActivities().stream().anyMatch(a -> a.getType() == ActivityType.WRITING);
        boolean hasSim     = step.getActivities().stream().anyMatch(a -> a.getType() == ActivityType.SIMULATION);

        // 이번 호출에서 처리해야 하는 대상 (MCQ → WRITING → SIMULATION)
        ActivityType pending =
                (hasMCQ && !mcqDone)        ? ActivityType.MCQ :
                        (hasWriting && !writingDone) ? ActivityType.WRITING :
                                (hasSim)                     ? ActivityType.SIMULATION : null;

        Boolean correct = null;
        String  feedback;
        ActivityType handledType = null;

        // 유틸: “비어있음” 판정 강화
        java.util.function.Predicate<String> isBlankStrong = s -> {
            String t = nullToEmpty(s).trim().toLowerCase();
            return t.isEmpty() || "null".equals(t) || "undefined".equals(t);
        };

        if (pending == ActivityType.MCQ) {
            if (mcqDone) {
                return WorkbookSubmitResponse.builder()
                        .stepIndex(stepIndex).nextStepIndex(stepIndex)
                        .isLastStep(stepIndex.equals(simRes.getActivities().size() - 1))
                        .handledType(null).correct(null)
                        .feedback("이미 선택형을 제출했습니다.")
                        .ok(true).nextExpectedType(hasWriting ? ActivityType.WRITING : (hasSim ? ActivityType.SIMULATION : null))
                        .shouldStartSimulation(false)
                        .build();
            }

            ActivityItem mcq = step.getActivities().stream()
                    .filter(a -> a.getType() == ActivityType.MCQ)
                    .findFirst().orElseThrow();

            String userAnswer = nullToEmpty(req.getUserAnswer()).trim();

            // 옵션 유효성 체크
            List<String> options = mcq.getOptions() == null ? Collections.emptyList() : mcq.getOptions();
            boolean optionsPresent = !options.isEmpty();
            boolean invalidByOptions = optionsPresent && options.stream().noneMatch(op -> op.equalsIgnoreCase(userAnswer));

            if (isBlankStrong.test(userAnswer) || invalidByOptions) {
                return WorkbookSubmitResponse.builder()
                        .stepIndex(stepIndex).nextStepIndex(stepIndex)
                        .isLastStep(stepIndex.equals(simRes.getActivities().size() - 1))
                        .handledType(null).correct(null)
                        .feedback(optionsPresent
                                ? "선택형에서 보기 중 하나를 선택해 주세요."
                                : "선택형 답을 먼저 선택해 주세요.")
                        .ok(true).nextExpectedType(ActivityType.MCQ)
                        .shouldStartSimulation(false)
                        .build();
            }

            handledType = ActivityType.MCQ;

            String userAnswerNorm    = normalizeAnswer(userAnswer);
            String optimalAnswerNorm = normalizeAnswer(mcq.getOptimal_option());

            if (!optimalAnswerNorm.isEmpty()) {
                correct  = optimalAnswerNorm.equals(userAnswerNorm);
                feedback = correct ? "정답입니다! ✅"
                        : "오답입니다. 정답은 \"" + nullToEmpty(mcq.getOptimal_option()) + "\" 입니다.";
            } else {
                correct  = null;
                feedback = "정답 기준이 없어 채점하지 않았어요.";
            }

            stepAnswerRepo.save(WorkbookStepAnswer.builder()
                    .workbookId(wb.getId())
                    .stepIndex(stepIndex)
                    .stepType(ActivityType.MCQ)
                    .answerText(userAnswer)
                    .mcqCorrect(correct)
                    .createdAt(LocalDateTime.now())
                    .build());
            mcqDone = true;

        } else if (pending == ActivityType.WRITING) {
            if (writingDone) {
                return WorkbookSubmitResponse.builder()
                        .stepIndex(stepIndex).nextStepIndex(stepIndex)
                        .isLastStep(stepIndex.equals(simRes.getActivities().size() - 1))
                        .handledType(null).correct(null)
                        .feedback("이미 서술형을 제출했습니다.")
                        .ok(true).nextExpectedType(hasSim ? ActivityType.SIMULATION : null)
                        .shouldStartSimulation(false)
                        .build();
            }

            String userAnswer = nullToEmpty(req.getUserAnswer()).trim();
            if (isBlankStrong.test(userAnswer)) {
                return WorkbookSubmitResponse.builder()
                        .stepIndex(stepIndex).nextStepIndex(stepIndex)
                        .isLastStep(stepIndex.equals(simRes.getActivities().size() - 1))
                        .handledType(null).correct(null)
                        .feedback("서술형 답변을 입력해 주세요.")
                        .ok(true).nextExpectedType(ActivityType.WRITING)
                        .shouldStartSimulation(false)
                        .build();
            }

            handledType = ActivityType.WRITING;
            ActivityItem writing = step.getActivities().stream()
                    .filter(a -> a.getType() == ActivityType.WRITING)
                    .findFirst().orElseThrow();

            String example = nullToEmpty(writing.getExample_answer());
            feedback = example.isBlank()
                    ? "좋은 시도예요. 핵심 키워드를 한두 줄 더 보완해보세요."
                    : "예시 답안 참고: " + example;

            stepAnswerRepo.save(WorkbookStepAnswer.builder()
                    .workbookId(wb.getId())
                    .stepIndex(stepIndex)
                    .stepType(ActivityType.WRITING)
                    .answerText(userAnswer)
                    .mcqCorrect(null)
                    .createdAt(LocalDateTime.now())
                    .build());
            writingDone = true;

        } else if (pending == ActivityType.SIMULATION) {
            handledType = null;
            feedback = "시뮬레이션을 시작하세요.";
        } else {
            handledType = null;
            feedback = "이 스텝은 완료되었습니다.";
        }

        // 저장 이후 재판정
        ActivityType nextPending =
                (hasMCQ && !mcqDone)        ? ActivityType.MCQ :
                        (hasWriting && !writingDone) ? ActivityType.WRITING :
                                (hasSim)                     ? ActivityType.SIMULATION : null;

        int lastIdx = simRes.getActivities().size() - 1;
        boolean isLast = (stepIndex == lastIdx);

        Integer nextStepIndex;
        boolean shouldStartSimulation = false;
        ActivityType nextExpectedType;

        if (nextPending == ActivityType.MCQ || nextPending == ActivityType.WRITING) {
            nextStepIndex = stepIndex;
            nextExpectedType = nextPending;
        } else if (nextPending == ActivityType.SIMULATION) {
            nextStepIndex = stepIndex;
            nextExpectedType = ActivityType.SIMULATION;
            shouldStartSimulation = true; // 프런트는 이때만 /sim/start
        } else {
            nextStepIndex = isLast ? null : stepIndex + 1;
            nextExpectedType = null;
            if (isLast) {
                String overall = generateOverallFeedback(wb);
                if (overall != null) feedback = overall;
            }
        }

        log.info("[WB-SUBMIT] userId={}, wbId={}, stepIndex={}, handledType={}, correct={}, next={}, nextExpected={}, startSim={}",
                userId, wb.getId(), stepIndex, handledType, correct, nextStepIndex, nextExpectedType, shouldStartSimulation);

        return WorkbookSubmitResponse.builder()
                .stepIndex(stepIndex)
                .nextStepIndex(nextStepIndex)
                .isLastStep(isLast)
                .handledType(handledType)
                .correct(correct)
                .feedback(feedback)
                .ok(true)
                .nextExpectedType(nextExpectedType)
                .shouldStartSimulation(shouldStartSimulation)
                .build();
    }

    /** ② 시뮬레이션 시작 */
    @Transactional
    public SimStartResponse startSimulation(SimStartRequest req) {
        final String userId = (req.getUserId() == null) ? "u001" : req.getUserId();
        final Long preferredId = (req.getWorkbookId() == null) ? null : req.getWorkbookId();

        // 1) 워크북 확보/파싱
        Workbook wb = getOrCreateDefaultWorkbook(userId, preferredId);
        if (wb.getRawJson() == null || wb.getRawJson().isBlank()) {
            throw new IllegalStateException("Workbook rawJson is empty");
        }

        final WorkbookSimulateResponse simRes;
        try {
            simRes = objectMapper.readValue(wb.getRawJson(), WorkbookSimulateResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse workbook.rawJson", e);
            throw new IllegalStateException("Invalid workbook JSON");
        }
        if (simRes.getActivities() == null || simRes.getActivities().isEmpty()) {
            throw new IllegalStateException("No activities in workbook JSON");
        }

        // 2) 시뮬레이션이 들어있는 스텝 인덱스 결정
        Integer reqIdx = req.getStepIndex();
        int stepIndex = resolveSimulationStepIndex(simRes, reqIdx);
        WorkbookActivity step = simRes.getActivities().get(stepIndex);

        // 3) 서브-액티비티 현황 점검
        boolean hasMCQ     = step.getActivities().stream().anyMatch(a -> a.getType() == ActivityType.MCQ);
        boolean hasWriting = step.getActivities().stream().anyMatch(a -> a.getType() == ActivityType.WRITING);
        boolean hasSim     = step.getActivities().stream().anyMatch(a -> a.getType() == ActivityType.SIMULATION);

        if (!hasSim) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This step has no SIMULATION item. stepIndex=" + stepIndex);
        }

        // 이미 저장된 답변 확인
        List<WorkbookStepAnswer> submitted = stepAnswerRepo.findByWorkbookIdAndStepIndex(wb.getId(), stepIndex);
        boolean mcqDone     = submitted.stream().anyMatch(a -> a.getStepType() == ActivityType.MCQ);
        boolean writingDone = submitted.stream().anyMatch(a -> a.getStepType() == ActivityType.WRITING);

        if (hasMCQ && !mcqDone) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "MCQ not completed yet for this step. Submit MCQ first.");
        }
        if (hasWriting && !writingDone) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "WRITING not completed yet for this step. Submit WRITING first.");
        }

        // 4) SIMULATION 아이템 추출
        ActivityItem sim = step.getActivities().stream()
                .filter(a -> a.getType() == ActivityType.SIMULATION)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("SIMULATION item not found at stepIndex=" + stepIndex));

        // 5) 세션 생성 및 초기 히스토리
        String sessionId = UUID.randomUUID().toString();

        String situation = nullToEmpty(sim.getSituation());
        if (situation.isBlank()) {
            situation = "상황: 아이와 대화를 시작해 보세요.";
        }

        String aiLine = extractChildLine(sim.getAi_optimal_response());
        if (aiLine == null || aiLine.isBlank()) {
            aiLine = "응, 이야기해줘.";
        }

        List<Map<String, String>> history = new ArrayList<>();
        history.add(Map.of("role", "ai", "text", aiLine));

        SimulationSession session = SimulationSession.builder()
                .sessionId(sessionId)
                .workbookId(wb.getId())
                .stepIndex(stepIndex)
                .userId(userId)
                .historyJson(writeJson(history))
                .finished(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sessionRepo.save(session);

        return SimStartResponse.builder()
                .sessionId(sessionId)
                .situation(situation)
                .aiLine(aiLine)
                .build();
    }

    /** ③ 시뮬레이션 다음 턴(부모 발화 → Python 호출 → 아이 반응) */
    @Transactional
    public SimNextResponse nextTurn(SimNextRequest req) {
        SimulationSession session = sessionRepo.findBySessionId(req.getSessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + req.getSessionId()));

        List<Map<String, String>> history = readHistory(session.getHistoryJson());
        String userReply = nullToEmpty(req.getUserReply());
        if (!userReply.isBlank()) {
            history.add(Map.of("role", "user", "text", userReply));
        }

        Workbook wb = workbookRepo.findById(session.getWorkbookId())
                .orElseThrow(() -> new NoSuchElementException("Workbook not found: " + session.getWorkbookId()));

        WorkbookActivity step = getStep(wb, session.getStepIndex());
        ActivityItem sim = step.getActivities().stream()
                .filter(a -> a.getType() == ActivityType.SIMULATION)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("SIMULATION item not found at stepIndex"));

        Map<String, Object> payload = Map.of(
                "topic", wb.getTopic(),
                "situation", nullToEmpty(sim.getSituation()),
                "history", history,
                "parent_reply", userReply
        );
        capstone.workbook.dto.SimNextResponse py = pythonClient.simNext(payload);

        String aiLine = nullToEmpty(py.getAiLine());
        if (!aiLine.isBlank()) {
            history.add(Map.of("role", "ai", "text", aiLine));
        }

        session.setHistoryJson(writeJson(history));
        session.setUpdatedAt(LocalDateTime.now());

        if (py.isFinished()) {
            session.setFinished(true);

            stepAnswerRepo.save(WorkbookStepAnswer.builder()
                    .workbookId(session.getWorkbookId())
                    .stepIndex(session.getStepIndex())
                    .stepType(ActivityType.SIMULATION)
                    .answerText(writeJson(history))
                    .mcqCorrect(null)
                    .createdAt(LocalDateTime.now())
                    .build());

            boolean isLast = Objects.equals(session.getStepIndex(), getLastIndex(wb));
            if (isLast) {
                String overall = generateOverallFeedback(wb);
                py.setFinalFeedback(overall != null ? overall : py.getFinalFeedback());
            }
        }

        sessionRepo.save(session);

        return SimNextResponse.builder()
                .aiLine(aiLine)
                .finished(py.isFinished())
                .finalFeedback(py.getFinalFeedback())
                .build();
    }

    // =========================
    // 유틸 & 보조 로직
    // =========================

    private WorkbookActivity getStep(Workbook wb, Integer stepIndex) {
        try {
            WorkbookSimulateResponse simRes = objectMapper.readValue(wb.getRawJson(), WorkbookSimulateResponse.class);

            if (simRes.getActivities() == null || simRes.getActivities().isEmpty())
                throw new IllegalStateException("No activities in workbook");

            if (stepIndex == null || stepIndex < 0 || stepIndex >= simRes.getActivities().size())
                throw new IndexOutOfBoundsException("Invalid stepIndex: " + stepIndex);

            return simRes.getActivities().get(stepIndex);
        } catch (Exception e) {
            log.error("getStep parse error", e);
            throw new IllegalStateException("Invalid workbook JSON");
        }
    }

    private Integer getLastIndex(Workbook wb) {
        try {
            WorkbookSimulateResponse simRes = objectMapper.readValue(wb.getRawJson(), WorkbookSimulateResponse.class);
            return (simRes.getActivities() == null || simRes.getActivities().isEmpty())
                    ? 0 : simRes.getActivities().size() - 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 저장된 답변(MCQ/WRITING/SIM 히스토리) 기반 최종 피드백 생성 */
    private String generateOverallFeedback(Workbook wb) {
        List<WorkbookStepAnswer> answers = stepAnswerRepo.findByWorkbookIdOrderByStepIndexAsc(wb.getId());

        List<Map<String, Object>> mcq = new ArrayList<>();
        List<Map<String, Object>> writing = new ArrayList<>();
        List<Map<String, String>> simHistory = new ArrayList<>();

        for (WorkbookStepAnswer a : answers) {
            if (a.getStepType() == ActivityType.MCQ) {
                WorkbookActivity step = getStep(wb, a.getStepIndex());
                String question = step.getActivities().stream()
                        .filter(it -> it.getType() == ActivityType.MCQ)
                        .findFirst().map(ActivityItem::getInstruction).orElse("");
                String optimal = step.getActivities().stream()
                        .filter(it -> it.getType() == ActivityType.MCQ)
                        .findFirst().map(ActivityItem::getOptimal_option).orElse("");
                mcq.add(Map.of(
                        "question", question,
                        "selected", nullToEmpty(a.getAnswerText()),
                        "optimal", optimal,
                        "correct", a.getMcqCorrect()
                ));
            } else if (a.getStepType() == ActivityType.WRITING) {
                WorkbookActivity step = getStep(wb, a.getStepIndex());
                String q = step.getActivities().stream()
                        .filter(it -> it.getType() == ActivityType.WRITING)
                        .findFirst().map(ActivityItem::getInstruction).orElse("");
                String ex = step.getActivities().stream()
                        .filter(it -> it.getType() == ActivityType.WRITING)
                        .findFirst().map(ActivityItem::getExample_answer).orElse("");
                writing.add(Map.of(
                        "question", q,
                        "answer", nullToEmpty(a.getAnswerText()),
                        "example", ex
                ));
            } else if (a.getStepType() == ActivityType.SIMULATION) {
                try {
                    List<Map<String, String>> hist = objectMapper.readValue(
                            nullToEmpty(a.getAnswerText()),
                            new TypeReference<List<Map<String, String>>>() {}
                    );
                    simHistory.addAll(hist);
                } catch (Exception ignore) {
                }
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("topic", wb.getTopic());
        payload.put("mcq", mcq);
        payload.put("writing", writing);
        payload.put("sim_history", simHistory);

        WorkbookFeedbackResponse res = pythonClient.finalFeedback(payload);
        return (res != null) ? res.getOverallComment() : null;
    }

    /** SIMULATION이 들어있는 스텝 인덱스를 결정 */
    private int resolveSimulationStepIndex(WorkbookSimulateResponse simRes, Integer requested) {
        int size = simRes.getActivities().size();

        if (requested != null && requested >= 0 && requested < size) {
            boolean hasSim = simRes.getActivities().get(requested).getActivities().stream()
                    .anyMatch(a -> a.getType() == ActivityType.SIMULATION);
            if (hasSim) return requested;
        }

        for (int i = 0; i < size; i++) {
            WorkbookActivity s = simRes.getActivities().get(i);
            if (s.getActivities() != null && s.getActivities().stream().anyMatch(a -> a.getType() == ActivityType.SIMULATION)) {
                return i;
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No step contains SIMULATION. activities=" + size + ", requested=" + requested);
    }

    /** "Child:" / "아이:" 라벨 제거 후 아이 첫 대사를 추출 */
    private String extractChildLine(String optimal) {
        if (optimal == null) return null;
        String[] lines = optimal.split("\\r?\\n");
        for (String raw : lines) {
            String t = raw.trim();
            if (t.regionMatches(true, 0, "Child:", 0, "Child:".length()) || t.startsWith("아이:")) {
                String text = t.replaceFirst("^(?i)Child:\\s*", "")
                        .replaceFirst("^아이:\\s*", "")
                        .trim();
                return text.isEmpty() ? null : text;
            }
        }
        return null;
    }

    private List<Map<String, String>> readHistory(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("Failed to read history json, start new", e);
            return new ArrayList<>();
        }
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    /** 기본 워크북 확보(없으면 Python simulate로 생성) — 단일 정의 */
    private Workbook getOrCreateDefaultWorkbook(String userId, Long preferredId) {

        // 1) preferredId 우선
        if (preferredId != null) {
            Optional<Workbook> byId = workbookRepo.findById(preferredId);
            if (byId.isPresent()) return byId.get();
        }

        // 2) 최신 워크북 사용
        List<Workbook> latest = workbookRepo.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        if (!latest.isEmpty()) return latest.get(0);

        // 3) 없으면 생성
        try {
            // Optional<UserProfile> 안전 처리
            Optional<capstone.support.userprofile.UserProfile> profileOpt = userProfileLoader.find(userId);

            Map<String, Object> userMap = new HashMap<>();
            profileOpt.ifPresent(p -> {
                userMap.put("child_age", p.getChildAge());
                userMap.put("parenting_style", p.getParentingStyle());
                userMap.put("parenting_goal", p.getParentingGoal());
                userMap.put("child_traits", p.getChildTraits());
                userMap.put("preferred_tone", p.getPreferredTone());
                userMap.put("language", p.getLanguage());
                userMap.put("allergies_or_health_issues", p.getHealthIssues());
            });

            WorkbookSimulateRequest simReq = WorkbookSimulateRequest.builder()
                    .topic("테스트 토픽")
                    .userId(userId)
                    .user(userMap)
                    .build();

            log.info("[WORKBOOK] simulate topic={}, userId={}, userKeys={}",
                    simReq.getTopic(), userId,
                    (simReq.getUser() == null ? "null" : simReq.getUser().keySet()));

            WorkbookSimulateResponse res = pythonClient.simulate(simReq);
            if (res == null) throw new IllegalStateException("Python simulate returned null");

            String json = objectMapper.writeValueAsString(res);

            Workbook created = Workbook.builder()
                    .userId(userId)
                    .topic(simReq.getTopic())
                    .activityCount(res.getActivities() == null ? 0 : res.getActivities().size())
                    .rawJson(json)
                    .createdAt(LocalDateTime.now())
                    .build();

            return workbookRepo.save(created);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to auto-generate workbook via Python", e);
        }
    }

    /** 공백 정규화 */
    private static String normalizeAnswer(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }
}
