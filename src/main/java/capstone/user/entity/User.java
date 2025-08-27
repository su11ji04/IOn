package capstone.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120, unique = true)
    private String email;

    @Column(nullable = false, length = 200)
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String parentNickname;

    @Column(length = 200)
    private String goal;

    @Column(length = 200)
    private String worry;

    //개인정보 동의
    @Column(nullable = false)
    private Integer personalInformationAgree;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Kid> kids = new ArrayList<>();

    public void addKid(Kid kid) {
        kids.add(kid);
        kid.setUser(this);
    }
}
