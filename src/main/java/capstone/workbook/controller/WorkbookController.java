package capstone.workbook.controller;

import capstone.web.CurrentUser;
import capstone.workbook.dto.*;
import capstone.workbook.service.WorkbookService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*") // 개발용
@RestController
@RequestMapping("/api/workbooks")
@RequiredArgsConstructor
public class WorkbookController {

    private final WorkbookService service;
    private final CurrentUser currentUser;

    // 워크북 생성 및 저장
    @PostMapping(
            value = "/{chapterId}/{workbookId}/create",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String,Object>> createWithReference(
            HttpServletRequest req,
            @PathVariable("chapterId") int chapterId,
            @PathVariable("workbookId") Long workbookId
    ) {
        String userId = currentUser.getUserId(req);
        Long id = service.createAndSave(userId, chapterId, workbookId);
        return ResponseEntity.ok(Map.of("id", id));
    }

    //워크북 챕터별 목록
    @GetMapping("/{chapterId}")
    public ResponseEntity<PagedListResponse<WorkbookListResponse>> listAll(
            HttpServletRequest req,
            @PathVariable("chapterId") int chapterId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        String userId = currentUser.getUserId(req);
        Page<WorkbookListResponse> p = service.list(userId, chapterId, PageRequest.of(page, size));
        return ResponseEntity.ok(new PagedListResponse<>(p.getContent(), p.getTotalPages(), p.getSize()));
    }


    // 워크북 내용 get
    @GetMapping("/{chapterId}/{workbookId}")
    public ResponseEntity<WorkbookRunResponse> runStartWorkbook(
            HttpServletRequest req,
            @PathVariable("chapterId") int chapterId,
            @PathVariable("workbookId") Long workbookId
    ) {
        String userId = currentUser.getUserId(req);
        WorkbookRunResponse body = service.buildRunPayload(chapterId, workbookId, userId);
        return ResponseEntity.ok(body);
    }

    // 워크북 답변 post
    @PostMapping(value = "/{chapterId}/{workbookId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> finishWorkbook(
            HttpServletRequest req,
            @PathVariable int chapterId,
            @PathVariable Long workbookId,
            @Valid @RequestBody WorkbookAnswerRequest request
    ) {
        String userId = currentUser.getUserId(req);
        service.saveAnswer(chapterId, workbookId, userId, request);
        return ResponseEntity.noContent().build();
    }

    // 워크북 피드백 get
    @GetMapping(value = "/{workbookId}/feedback")
    public ResponseEntity<WorkbookFeedbackDto> feedbackWorkbook(
            HttpServletRequest req,
            @PathVariable("workbookId") Long workbookId
    ) {
        String userId = currentUser.getUserId(req);
        WorkbookFeedbackDto body = service.generateAndSaveFeedback(workbookId, userId);
        return ResponseEntity.ok(body);
    }
}