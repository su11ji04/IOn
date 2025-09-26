// capstone.workbook.entity.SimulationSession.java
package capstone.workbook.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workbook_sim_session")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SimulationSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, unique = true, nullable = false)
    private String sessionId;

    private Long workbookId;
    private Integer stepIndex;
    private String userId;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String historyJson; // [{role:"ai"|"user", "text":"..."}...]

    private boolean finished;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
