package capstone.workbook.controller;

import capstone.workbook.dto.CreateWorkbookRequest;
import capstone.workbook.dto.ListResponse;
import capstone.workbook.dto.RunDtos;
import capstone.workbook.dto.WorkbookDetailResponse;
import capstone.workbook.service.WorkbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@CrossOrigin(origins = "*") //개발용
@RestController
@RequestMapping("/api/workbooks")
@RequiredArgsConstructor
public class WorkbookController {

    private final WorkbookService service;

    // 워크북 생성 + 저장
    @PostMapping
    public Map<String, Object> create(@RequestBody CreateWorkbookRequest req) {
        Long id = service.createAndSave(req);
        return Map.of("id", id);
    }

    // 워크북 목록 조회
    @GetMapping
    public ListResponse list(
            @RequestParam(name = "userId", required = false) String userId,
            Pageable pageable
    ) {
        return service.listSimple(userId, pageable);
    }

    // [개발용]
    @GetMapping("/{id}")
    public WorkbookDetailResponse get(@PathVariable("id") Long id) throws IOException {
        return service.getOne(id);
    }

}