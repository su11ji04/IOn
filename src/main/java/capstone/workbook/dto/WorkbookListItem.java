package capstone.workbook.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookListItem {
    private Long id;
    private String topic;
    private Integer activityCount;
    private LocalDateTime createdAt;
}
