package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Frequency {
    private Integer parentFrequency;   // 0~100
    private Integer kidFrequency;      // 0~100
    @Column(length = 1000)
    private String frequencyFeedback;
}

