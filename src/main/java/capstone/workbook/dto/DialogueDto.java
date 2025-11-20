package capstone.workbook.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DialogueDto {
    private String userLine;
    private String aiLine;
}
