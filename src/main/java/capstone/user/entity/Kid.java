package capstone.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kids")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Kid {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //어느 부모에게 속해 있는가
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false)
    private Integer age;

    @Column(length = 50)
    private String tendency;

    @Column(length = 300)
    private String note;
}
