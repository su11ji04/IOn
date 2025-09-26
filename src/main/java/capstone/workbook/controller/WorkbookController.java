package capstone.workbook.controller;

import capstone.workbook.dto.*;
import capstone.workbook.service.WorkbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workbook")
@RequiredArgsConstructor
public class WorkbookController {

    private final WorkbookService workbookService;

    // 선택형/작성형 제출
    @PostMapping(value = "/submit", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public WorkbookSubmitResponse submit(@RequestBody @Valid WorkbookSubmitRequest req) {
        req.setUserId("u001");
        req.setWorkbookId(1L);
        return workbookService.submit(req);
    }

    // 시뮬레이션 시작
    @PostMapping(value = "/sim/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SimStartResponse startSim(@RequestBody @Valid SimStartRequest req) {
        req.setUserId("u001");
        req.setWorkbookId(1L);
        return workbookService.startSimulation(req);
    }

    // 시뮬레이션 다음 턴
    @PostMapping(value = "/sim/next", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SimNextResponse nextSim(@RequestBody @Valid SimNextRequest req) {
        return workbookService.nextTurn(req);
    }
}
