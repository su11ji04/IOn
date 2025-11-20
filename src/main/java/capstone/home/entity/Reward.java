package capstone.home.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reward {
    @Column(name = "reward_id")
    private Integer rewardId;
    @Column(name = "earned_at", length = 100)
    private String earnedAt; 
}
