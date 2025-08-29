package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeProposal {

    @Column(name = "existing_expression", length = 1000)
    private String existingExpression;

    @Column(name = "proposal_expression", length = 1000)
    private String proposalExpression;
}

