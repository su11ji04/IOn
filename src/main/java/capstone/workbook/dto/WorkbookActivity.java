package capstone.workbook.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookActivity {
    private String activity_title;
    private List<ActivityItem> activities; // 순서: MCQ -> WRITING -> SIMULATION
}
