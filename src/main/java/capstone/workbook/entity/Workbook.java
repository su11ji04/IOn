package capstone.workbook.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workbook_simulation")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder

public class Workbook {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String topic;

    private Integer activityCount;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String rawJson;

    private LocalDateTime createdAt;
}
