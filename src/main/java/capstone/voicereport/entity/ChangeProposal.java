package capstone.voicereport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeProposal {
    @Column(length = 1000)
    private String existingExpression;
    @Column(length = 1000)
    private String proposalExpression;
}
