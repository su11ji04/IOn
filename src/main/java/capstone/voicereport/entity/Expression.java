package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expression {

    @Column(name = "parent_expression", length = 1000)
    private String parentExpression;

    @Column(name = "kid_expression", length = 1000)
    private String kidExpression;

    @Column(name = "parent_conditions", length = 1000)
    private String parentConditions;

    @Column(name = "kid_conditions", length = 1000)
    private String kidConditions;

    @Column(name = "expression_feedback", length = 1000)
    private String expressionFeedback;
}
