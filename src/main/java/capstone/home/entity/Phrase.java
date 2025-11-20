package capstone.home.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "phrase")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Phrase {
    @Id
    @Column(name = "phrase_id")
    private Integer phraseId;
    @Column(name = "phrase_content", length = 1000)
    private String phraseContent;
}
