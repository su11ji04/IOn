package capstone.voicereport.entity;

import capstone.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "voice_report")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoiceReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "audio_original_name", length = 255)
    private String audioOriginalName;

    @Column(name = "audio_size") // length 속성 제거(숫자형엔 의미 없음)
    private long audioSize;

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Column(name = "sub_title", length = 200)
    private String subTitle;

    @Column(name = "report_day", length = 30)
    private String day;

    @PrePersist
    public void prePersist() {
        if (this.day == null) {
            this.day = java.time.LocalDate.now().toString();
        }
    }

    @Column(name = "conversation_summary", length = 4000)
    private String conversationSummary;

    @Column(name = "length_seconds")
    private Integer lengthSeconds;

    @Column(name = "overall_feedback", length = 2000)
    private String overallFeedback;

    // === Embedded: Frequency ===
    @Embedded
    private Frequency frequency;

    // === Embedded: Expression ===
    @Embedded
    private Expression expression;

    // === Emotion timeline ===
    @ElementCollection
    @CollectionTable(name = "voice_report_emotion_timeline",
            joinColumns = @JoinColumn(name = "report_id"))
    private List<EmotionPoint> emotionTimeline = new ArrayList<>();

    @Column(name = "emotion_feedback", length = 2000)
    private String emotionFeedback;

    @Column(name = "kid_attitude", length = 500)
    private String kidAttitude;

    @ElementCollection
    @CollectionTable(name = "voice_report_change_proposal",
            joinColumns = @JoinColumn(name = "report_id"))
    private List<ChangeProposal> changeProposals = new ArrayList<>();

    @Column(name = "pattern", length = 1000)
    private String pattern;

    @Column(name = "strength", length = 1000)
    private String strength;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
