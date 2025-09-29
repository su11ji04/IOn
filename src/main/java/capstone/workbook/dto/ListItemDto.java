package capstone.workbook.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListItemDto {
    private Long id;
    private String topic;
    private Integer activityCount;
    private LocalDateTime createdAt;
}
