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

    private String userId;          // ex) u004
    private String topic;           // ex) 나를 아는 부모

    private Integer activityCount;  // 활동 개수(미리보기/검색용)

    @Lob
    @Column(columnDefinition = "CLOB")
    private String rawJson;         // Python 응답 전체(JSON 문자열)

    private LocalDateTime createdAt;
}
