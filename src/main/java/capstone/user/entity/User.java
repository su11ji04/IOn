package capstone.user.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "`user`", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @Column(name = "email", length = 320, nullable = false)
    private String email;

    @NotBlank
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "user_image", length = 3000)
    private String userImage;

    @Column(name = "parent_name", length = 2000)
    private String parentName;

    @Column(name = "parent_nickname", length = 2000)
    private String parentNickname;

    @Column(name = "kids_id")
    private Integer kidsId;

    @Column(name = "kids_nickname", length = 2000)
    private String kidsNickname;

    @Column(name = "kids_age")
    private Integer kidsAge;

    @Column(name = "kids_tendency", length = 2000)
    private String kidsTendency;

    @Column(name = "kids_note", length = 2000)
    private String kidsNote;

    @Column(name = "goal", length = 2000)
    private String goal;

    @Column(name = "worry", length = 2000)
    private String worry;

    @Column(name = "personal_information_agree")
    private Integer personalInformationAgree;

    @Column(name = "now_chapter")
    private Integer nowChapter;
}

