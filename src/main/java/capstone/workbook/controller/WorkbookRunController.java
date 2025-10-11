package capstone.workbook.controller;

import capstone.web.CurrentUser;
import capstone.workbook.dto.RunDtos;
import capstone.workbook.dto.RunDtos.*;
import capstone.workbook.service.WorkbookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workbooks")
@RequiredArgsConstructor
public class WorkbookRunController {

    private final WorkbookService service;
    private final CurrentUser currentUser;

    // 선택형
    @PostMapping(value="/{id}/run/start", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StartResponse> start(
            HttpServletRequest req,
            @PathVariable("id") Long workbookId
    ) throws Exception {
        String userId = currentUser.getUserId(req);
        return ResponseEntity.ok(service.runStart(workbookId, userId));
    }

    // 선택형 답변 제출 + 작성형
    @PostMapping(
            value="/runs/{runId}/answer/mcq",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mcq(
            @PathVariable("runId") Long runId,
            @RequestBody RunDtos.McqAnswerRequest reqBody
    ) throws Exception {
        var writing = service.answerMcq(runId, reqBody.getAnswer());
        return ResponseEntity.ok(writing);
    }

    // 작성형 답변 제출 + 시뮬레이션
    @PostMapping(
            value="/runs/{runId}/answer/writing",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> writing(
            @PathVariable("runId") Long runId,
            @RequestBody RunDtos.WritingAnswerRequest reqBody
    ) throws Exception {
        var sim = service.answerWriting(runId, reqBody.getText());
        return ResponseEntity.ok(sim);
    }
}
