package capstone.workbook.service;

import capstone.workbook.dto.WorkbookActivity;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookSimulateResponse {
    private List<WorkbookActivity> activities;
}
