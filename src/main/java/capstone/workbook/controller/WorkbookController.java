package capstone.workbook.controller;

import capstone.workbook.dto.*;
import capstone.workbook.service.WorkbookService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workbooks")
public class WorkbookController {

    private final WorkbookService workbookService;

    // 공통: 세션에서 userId 꺼내고 없으면 예외
    private Integer requireUserId(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            // 여기서 원하는 예외 타입/메시지로 바꾸면 됨
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return userId;
    }

    // 현재 Chapter 이동
    @GetMapping("/chapter")
    public ResponseEntity<ChapterIdDto> getNowChapter(HttpSession session) {
        Integer userId = requireUserId(session);
        ChapterIdDto dto = workbookService.getChapter(userId);
        return ResponseEntity.ok(dto);
    }

    // Chapter Theory
    @GetMapping("/{chapterId}/theory")
    public ResponseEntity<TheoryDto> getTheory(HttpSession session,
                                               @PathVariable int chapterId) {
        Integer userId = requireUserId(session);
        TheoryDto t = workbookService.getChapterTheory(chapterId, userId);
        return ResponseEntity.ok(t);
    }

    // Chapter 페이지 조회
    @GetMapping("/{chapterId}")
    public ResponseEntity<WorkbookListResponse> getChapterPage(
            HttpSession session,
            @PathVariable int chapterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Integer userId = requireUserId(session);
        // page/size는 나중에 페이징 붙일 때 사용 가능
        WorkbookListResponse response = workbookService.getChapterPageContent(userId, chapterId);
        return ResponseEntity.ok(response);
    }

    // Get Workbook
    @GetMapping("/{chapterId}/{lessonId}")
    public ResponseEntity<WorkbookDto> getWorkbook(
            HttpSession session,
            @PathVariable int chapterId,
            @PathVariable int lessonId
    ) {
        Integer userId = requireUserId(session);
        WorkbookDto workbookDto = workbookService.getWorkbookContent(userId, chapterId, lessonId);
        return ResponseEntity.ok(workbookDto);
    }

    // Post Workbook Answer
    @PostMapping("/{chapterId}/{lessonId}")
    public ResponseEntity<Void> postWorkbookAnswer(
            HttpSession session,
            @PathVariable int chapterId,
            @PathVariable int lessonId,
            @RequestBody WorkbookAnswerDto answerDto
    ) {
        Integer userId = requireUserId(session);
        workbookService.postWorkbookContent(userId, chapterId, lessonId, answerDto);
        return ResponseEntity.ok().build();
    }

    // Get Workbook Feedback
    @GetMapping("/{chapterId}/{lessonId}/feedback")
    public ResponseEntity<WorkbookFeedbackDto> getWorkbookFeedback(
            HttpSession session,
            @PathVariable int chapterId,
            @PathVariable int lessonId
    ) {
        Integer userId = requireUserId(session);
        WorkbookFeedbackDto workbookFeedbackDto =
                workbookService.getWorkbookFeedback(userId, chapterId, lessonId);
        return ResponseEntity.ok(workbookFeedbackDto);
    }

    // Get Simulation
    @GetMapping("/{chapterId}/simulation")
    public ResponseEntity<SimulationDto> getSimulation(HttpSession session,
                                                       @PathVariable int chapterId) {
        Integer userId = requireUserId(session);
        SimulationDto dto = workbookService.getSimulationContent(userId, chapterId);
        return ResponseEntity.ok(dto);
    }

    // Simulation Next Line
    @PostMapping("/{chapterId}/getLine")
    public ResponseEntity<NextLineDto> getSimulationLine(
            HttpSession session,
            @PathVariable int chapterId,
            @RequestBody Dialogues dialogues
    ) {
        Integer userId = requireUserId(session);
        NextLineDto nextLineDto = workbookService.getSimulationNextLine(userId, chapterId, dialogues);
        return ResponseEntity.ok(nextLineDto);
    }

    // Simulation Feedback
    @GetMapping("/{chapterId}/simulationFeedback")
    public ResponseEntity<SimulationFeedbackDto> getSimulationFeedback(
            HttpSession session,
            @PathVariable int chapterId
    ) {
        Integer userId = requireUserId(session);
        SimulationFeedbackDto simulationFeedbackDto =
                workbookService.getSimulationFeedback(userId, chapterId);
        return ResponseEntity.ok(simulationFeedbackDto);
    }

}
