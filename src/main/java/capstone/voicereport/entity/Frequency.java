package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Frequency {

    //부모님 발화 빈도
    @Column(name = "parent_frequency")
    private Integer parentFrequency;

    //아이의 발화 빈도
    @Column(name = "kid_frequency")
    private Integer kidFrequency;

    //발화 빈도 관련 피드백
    @Column(name = "frequency_feedback", length = 1000)
    private String frequencyFeedback;
}
