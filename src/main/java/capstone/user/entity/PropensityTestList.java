package capstone.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "propensity_test_list")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PropensityTestList {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "propensity_test_id")
        private int propensityTestId;

        @Column(name = "propensity_test_question", length = 2000)
        private String propensityTestQuestion;
}
