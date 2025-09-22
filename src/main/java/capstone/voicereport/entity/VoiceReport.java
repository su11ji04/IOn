package capstone.voicereport.entity;

import capstone.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "voice_report",
        indexes = {
                @Index(name = "idx_voice_report_user", columnList = "user_id"),
                @Index(name = "idx_voice_report_created_at", columnList = "created_at")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoiceReport {

    // 보이스리포트 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // USER 정보
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    private User user;

    // 음성 파일 이름
    @Column(name = "audio_original_name", length = 255)
    private String audioOriginalName;

    // 음성 파일 사이즈
    @Column(name = "audio_size")
    private Long audioSize;

    // 음성 파일 경로
    @Column(name = "audio_path", length = 500)
    private String audioPath;

    // 보이스리포트 부제목
    @Column(name = "sub_title", length = 200)
    private String subTitle;

    // 보이스리포트 생성일
    @Column(name = "report_day")
    private LocalDate day;

    @PrePersist
    public void prePersist() {
        if (this.day == null) {
            this.day = LocalDate.now();
        }
    }

    // 대화 요약
    @Column(name = "conversation_summary", length = 4000)
    private String conversationSummary;

    // 상호작용 전반 피드백
    @Column(name = "overall_feedback", length = 2000)
    private String overallFeedback;

    // 상황 / 표현 분석
    @Embedded
    private Expression expression;

    // 대체 표현 제안
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "voice_report_change_proposal",
            joinColumns = @JoinColumn(name = "report_id"))
    private List<ChangeProposal> changeProposals = new ArrayList<>();

    // 부모님의 감정 분석
    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "voice_report_emotion_timeline",
            joinColumns = @JoinColumn(name = "report_id"))
    @OrderColumn(name = "seq")
    private List<EmotionPoint> emotionTimeline = new ArrayList<>();

    @Column(name = "emotion_feedback", length = 2000)
    private String emotionFeedback;

    // 아이의 대화 태도 분석
    @Column(name = "kid_attitude", length = 500)
    private String kidAttitude;

    // 발화 빈도 분석
    @Embedded
    private Frequency frequency;

    // 강점 분석
    @Column(name = "strength", length = 1000)
    private String strength;

    // 보이스리포트 생성 시각(정렬용)
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
