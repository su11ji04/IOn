package capstone.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TestListScore {

    @Column(name = "propensity_test_id", nullable = false)
    private int propensityTestId;

    @Column(name = "propensity_test_score", nullable = false)
    private int propensityTestScore;
}
