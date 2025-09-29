package capstone.voicereport.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class CreateVoiceReportRequest {
    private String userId;
}