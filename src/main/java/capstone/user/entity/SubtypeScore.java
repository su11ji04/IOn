package capstone.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SubtypeScore {

    @Column(name = "sub_type", length = 100, nullable = false)
    private String type;

    @Column(name = "sub_type_score", nullable = false)
    private double score;
}
