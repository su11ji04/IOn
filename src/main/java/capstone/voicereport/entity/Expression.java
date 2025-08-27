package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expression {
    @Column(length = 1000)
    private String parentExpression;
    @Column(length = 1000)
    private String kidExpression;

    @Column(length = 1000)
    private String parentConditions;
    @Column(length = 1000)
    private String kidConditions;

    @Column(length = 1000)
    private String expressionFeedback;
}
