package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeProposal {

    //기존 표현
    @Column(name = "existing_expression", length = 1000)
    private String existingExpression;

    //제안 표현
    @Column(name = "proposal_expression", length = 1000)
    private String proposalExpression;
}

