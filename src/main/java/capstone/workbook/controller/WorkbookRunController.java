package capstone.workbook.controller;

import capstone.workbook.dto.RunDtos.*;
import capstone.workbook.service.WorkbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*") //개발용
@RestController
@RequestMapping("/api/workbooks")
@RequiredArgsConstructor
public class WorkbookRunController {

    private final WorkbookService service;

    @PostMapping("/{id}/run/start")
    public StartResponse start(@PathVariable("id") Long workbookId) throws Exception {
        return service.runStart(workbookId, "u001");
    }

    @PostMapping("/runs/{runId}/answer/mcq")
    public Map<String,Object> mcq(@PathVariable("runId") Long runId,
                                  @RequestBody McqAnswerRequest req) throws Exception {
        return service.answerMcq(runId, req.getAnswer());
    }

    @PostMapping("/runs/{runId}/answer/writing")
    public Map<String,Object> writing(@PathVariable("runId") Long runId,
                                      @RequestBody WritingAnswerRequest req) throws Exception {
        return service.answerWriting(runId, req.getText());
    }

    @PostMapping("/runs/{runId}/sim/next")
    public Map<String,Object> simNext(@PathVariable("runId") Long runId,
                                      @RequestBody SimNextRequest req) throws Exception {
        return service.simNext(runId, req.getParentReply());
    }

    @PostMapping("/runs/{runId}/feedback")
    public Map<String,Object> feedback(@PathVariable("runId") Long runId) throws Exception {
        return service.finalizeFeedback(runId);
    }
}
