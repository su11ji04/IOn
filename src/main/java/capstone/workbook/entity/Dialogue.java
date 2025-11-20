package capstone.workbook.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Embeddable
public class Dialogue {

    @Lob
    @Column(name = "user_line", columnDefinition = "TEXT")
    private String userLine;
    @Lob
    @Column(name = "ai_line", columnDefinition = "TEXT")
    private String aiLine;
}
