package capstone.workbook.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "workbook",
        indexes = {
                @Index(name = "idx_workbook_user", columnList = "user_id"),
                @Index(name = "idx_workbook_created_at", columnList = "created_at")
        }
)
public class Workbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 100, nullable = false)
    private String userId;

    @Column(name = "activity_title", length = 200, nullable = false)
    private String activityTitle;

    @Column(name = "activity_count", nullable = false)
    private Integer activityCount;

    @Lob
    @Column(name = "activity_json", nullable=false)
    private String activityJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

}
