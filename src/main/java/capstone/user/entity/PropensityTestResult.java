package capstone.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "propensity_test_result")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PropensityTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private int resultId;
    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "user_type", length = 50, nullable = false)
    private String userType;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "propensity_test_list_scores",
            joinColumns = @JoinColumn(name = "result_id")
    )
    private List<TestListScore> listScores = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "propensity_test_scores",
            joinColumns = @JoinColumn(name = "result_id")
    )
    @MapKeyColumn(name = "test_scores_key")   // 'authoritative' 등
    @Column(name = "test_scores")
    private Map<String, Double> testScores = new HashMap<>();


    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "propensity_test_subtype",
            joinColumns = @JoinColumn(name = "result_id")
    )
    private List<SubtypeScore> subtype = new ArrayList<>();
}
