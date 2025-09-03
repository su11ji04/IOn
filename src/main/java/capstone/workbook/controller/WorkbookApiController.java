package capstone.workbook.controller;

import capstone.support.userprofile.UserProfileLoader;
import capstone.workbook.dto.WorkbookSimulateRequest;
import capstone.workbook.dto.WorkbookSimulateResponse;
import capstone.workbook.service.WorkbookService; // ← 이걸로 교체
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workbook")
public class WorkbookApiController {

    private final WorkbookService service;
    private final UserProfileLoader userProfileLoader;

    @PostMapping("/simulate")
    public ResponseEntity<WorkbookSimulateResponse> simulate(
            @RequestBody WorkbookSimulateRequest req,
            @RequestParam(name = "save", defaultValue = "false") boolean save
    ) {
        if (req.getUser() == null || req.getUser().isEmpty()) {
            String id = (req.getUserId() == null || req.getUserId().isBlank()) ? "u001" : req.getUserId();
            var up = userProfileLoader.find(id).orElseThrow(
                    () -> new IllegalArgumentException("user not found in CSV: " + id));
            req.setUser(userProfileLoader.toPythonMap(up));   // ← 여기서 반드시 세팅
        }
        return ResponseEntity.ok(service.simulateAndMaybeSave(req, save));
    }
}
