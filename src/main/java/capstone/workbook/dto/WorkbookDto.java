package capstone.workbook.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkbookDto {
    private Integer workbookId;
    private Integer done; // 0->시작 전 1->워크북 활동 완료

    private Integer userId;

    private Integer chapterId;
    private String chapterTitle;
    private Integer lessonId;
    private String lessonTitle;

    // 작성형
    private String descriptiveFormQuestion;
    private String descriptiveFormAnswer;
    private String descriptiveFormExample;

    // 선택형
    private String selectiveQuestion;
    private List<String> selectiveOptions;
    private String selectiveAnswer;
    private String selectiveExample;

    private String workbookFeedback;
}
