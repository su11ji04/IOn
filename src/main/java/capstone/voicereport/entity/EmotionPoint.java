package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmotionPoint {

    @Column(name = "time", length = 30) // DDL 컬럼명: time
    private String time;

    @Column(name = "moment_emotion", length = 100)
    private String momentEmotion;
}



