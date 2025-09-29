package capstone.workbook.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkbookDetailResponse {
    private Long id;
    private String userId;
    private String topic;
    private Integer activityCount;
    private LocalDateTime createdAt;
    private Map<String,Object> activity;
}
