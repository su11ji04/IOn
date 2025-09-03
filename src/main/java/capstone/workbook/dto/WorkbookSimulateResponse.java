package capstone.workbook.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookSimulateResponse {
    private List<WorkbookActivity> activities;
}
