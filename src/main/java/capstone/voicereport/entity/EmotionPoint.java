package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmotionPoint {
    @Column(length = 30)
    private String time; // "00:23" 같은 문자열
    @Column(length = 100)
    private String momentEmotion; // "기쁨/화남/..." 등
}
