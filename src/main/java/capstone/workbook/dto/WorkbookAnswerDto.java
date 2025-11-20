package capstone.workbook.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkbookAnswerDto {
    private String descriptiveFormAnswer;
    private String selectiveAnswer;
}
