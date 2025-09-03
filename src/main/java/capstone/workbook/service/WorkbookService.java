package capstone.workbook.service;

import capstone.workbook.dto.WorkbookSimulateRequest;
import capstone.workbook.dto.WorkbookSimulateResponse;
import capstone.workbook.entity.Workbook;
import capstone.workbook.repository.WorkbookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkbookService {

    private final WorkbookPythonClient pythonClient;
    private final WorkbookRepository repo;
    private final ObjectMapper objectMapper;

    public WorkbookSimulateResponse simulateAndMaybeSave(WorkbookSimulateRequest req, boolean save) {
        WorkbookSimulateResponse res = pythonClient.simulate(req);

        if (save) {
            try {
                int cnt = res.getActivities() == null ? 0 : res.getActivities().size();
                String json = objectMapper.writeValueAsString(res);

                Workbook entity = Workbook.builder()
                        .userId(req.getUserId())
                        .topic(req.getTopic())
                        .activityCount(cnt)
                        .rawJson(json)
                        .createdAt(LocalDateTime.now())
                        .build();
                repo.save(entity);
            } catch (Exception e) {
                // 저장 실패해도 응답은 주도록 (로그만)
                e.printStackTrace();
            }
        }
        return res;
    }
}
