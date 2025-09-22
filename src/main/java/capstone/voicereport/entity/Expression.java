package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expression {
    //부모님 표현
    @Column(name = "parent_expression", length = 1000)
    private String parentExpression;

    //부모님 상황 분석
    @Column(name = "parent_conditions", length = 1000)
    private String parentConditions;

    //아이 표현
    @Column(name = "kid_expression", length = 1000)
    private String kidExpression;

    //아이의 상황 분석
    @Column(name = "kid_conditions", length = 1000)
    private String kidConditions;

    //상황 및 표현 관련 피드백
    @Column(name = "expression_feedback", length = 1000)
    private String expressionFeedback;
}
