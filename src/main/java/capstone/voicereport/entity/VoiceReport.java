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

    @Column(length = 255)
    private String audioOriginalName;
    @Column(length = 100)
    private long audioSize;
    @Column(length = 500)
    private String audioPath;

    @Column(length = 200)
    private String subTitle;

    @Column(name = "report_day", length = 30)
    private String day;
    @PrePersist
    public void prePersist() {
        if (this.day == null) {
            this.day = java.time.LocalDate.now().toString();
        }
    }

    @Column(length = 4000)
    private String conversationSummary;

    private Integer lengthSeconds;

    @Column(length = 2000)
    private String overallFeedback;

    @Embedded
    private Frequency frequency;

    @Embedded
    private Expression expression;

    @ElementCollection
    @CollectionTable(name = "voice_report_emotion_timeline", joinColumns = @JoinColumn(name = "report_id"))
    private List<EmotionPoint> emotionTimeline = new ArrayList<>();

    @Column(length = 2000)
    private String emotionFeedback;

    @Column(length = 500)
    private String kidAttitude;

    @ElementCollection
    @CollectionTable(name = "voice_report_change_proposal", joinColumns = @JoinColumn(name = "report_id"))
    private List<ChangeProposal> changeProposals = new ArrayList<>();

    @Column(length = 1000)
    private String pattern;

    @Column(length = 1000)
    private String strength;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
