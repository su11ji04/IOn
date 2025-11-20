package capstone.workbook.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonDto {
    private int lessonId;
    private String lessonTitle;
    private int progressStatus; // 1=완료, 0=미완
}
