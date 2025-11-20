package capstone.voicereport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceReportListResponse {
    private int reportId;
    private String subTitle;
    private String day;
}
