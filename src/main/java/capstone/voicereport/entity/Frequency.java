package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Frequency {

    @Column(name = "parent_frequency")
    private Integer parentFrequency;

    @Column(name = "kid_frequency")
    private Integer kidFrequency;

    @Column(name = "frequency_feedback", length = 1000)
    private String frequencyFeedback;
}
