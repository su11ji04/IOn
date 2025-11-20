package capstone.home.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(
        name = "user_profile",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Integer profileId;   // 프로필 고유 ID
    @Column(name = "user_id", nullable = false)
    private Integer userId; // 외래키

    @Column(name = "user_image")
    private String userImage; //사용자 사진
    @Column(name = "level")
    private Integer level; // 사용자 레벨
    @Column(name = "points")
    private Integer points; // 포인트
    @Column(name = "parent_nickname", length = 255)
    private String parentNickname; // 부모 닉네임

    @Column(name = "streak_day")
    private Integer streakDay; // 연속 접속일
    @Column(name = "phrase")
    private String phrase; // 명언

    @Column(name = "month_frequency")
    private Integer monthFrequency; //이번달 접속일
    @Column(name = "voicereport_frequency")
    private Integer voicereportFrequency; // 이번달 보이스리포트 이용 횟수
    @Column(name = "chatbot_frequency")
    private Integer chatBotFrequency; // 이번달 챗봇 이용 횟수
    @Column(name = "workbook_frequency")
    private Integer workBookFrequency; // 이번달 진행 워크북 수 (ex. 03 Lesson)

    @Column(name = "message", length = 500)
    private String message; // 제안 메시지

    // 리워드 목록 (별도 테이블 매핑)
    @ElementCollection
    @CollectionTable(
            name = "user_profile_reward",
            joinColumns = @JoinColumn(name = "profile_id")
    )
    private List<Reward> reward;

    @Column(name = "voicereport_first")
    private Integer usedVoiceReportOnce; // 1이면 보이스리포트 최초 이용
    @Column(name = "workbook_first")
    private Integer finishedWorkbookOnce; // 1이면 워크북 최초 이용
    @Column(name = "chatbot_first")
    private Integer usedChatbotOnce; // 1이면 챗봇 최초 이용
    @Column(name = "last_login_date")
    private LocalDate lastLoginDate; //마지막 접속일
    @Column(name = "last_login_month", length = 7)
    private String lastLoginMonth;  // 이번달 접속일 계산용
}
