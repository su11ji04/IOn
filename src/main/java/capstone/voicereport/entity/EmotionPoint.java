package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmotionPoint {
    // 시간
    @Column(name = "emotion_time_label", length = 30)
    private String time;

    // 감정
    @Column(name = "moment_emotion", length = 100)
    private String momentEmotion;
}



