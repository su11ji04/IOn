package capstone.voicereport.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(name = "user_id", length = 100)
    private int userId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private int reportId;

    @Column(name = "sub_title", length = 200)
    private String subTitle;

    @Column(name = "report_day")
    private String day;

    @Column(name = "conversation_summary", length = 4000)
    private String conversationSummary;

    @Column(name = "overall_feedback", length = 2000)
    private String overallFeedback;

    @Embedded
    private Expression expression;

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "voice_report_change_proposal",
            joinColumns = @JoinColumn(name = "report_id")
    )
    private List<ChangeProposal> changeProposals = new ArrayList<>();

    @Builder.Default
    @ElementCollection
    @CollectionTable(
            name = "voice_report_emotion_timeline",
            joinColumns = @JoinColumn(name = "report_id")
    )
    @OrderColumn(name = "seq")
    private List<EmotionPoint> emotionTimeline = new ArrayList<>();

    @Column(name = "emotion_feedback", length = 2000)
    private String emotionFeedback;

    @Column(name = "kid_attitude", length = 500)
    private String kidAttitude;

    @Embedded
    private Frequency frequency;

    @Column(name = "strength", length = 1000)
    private String strength;

    // 정렬용
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
