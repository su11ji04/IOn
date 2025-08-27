package capstone.voicereport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateVoiceReportRequest {
    private Long userId;
    @NotBlank
    private String subTitle;
}

