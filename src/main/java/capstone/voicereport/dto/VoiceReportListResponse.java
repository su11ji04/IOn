package capstone.voicereport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceReportListResponse {
    private Integer id;
    private String subTitle;
    private String day;
}
