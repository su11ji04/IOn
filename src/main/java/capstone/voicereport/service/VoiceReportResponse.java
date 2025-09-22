package capstone.voicereport.service;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoiceReportResponse {

    private Long id;
    private String subTitle;
    private String day;

    private String conversationSummary;
    private String overallFeedback;

    private Expression expression;
    private List<ChangeProposal> changeProposal;

    private Emotion emotion;
    private String kidAttitude;

    private Frequency frequency;
    private String strength;



    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Expression {
        private String parentExpression;
        private String kidExpression;
        private String parentConditions;
        private String kidConditions;
        private String expressionFeedback;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ChangeProposal {
        private String existingExpression;
        private String proposalExpression;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Emotion {
        private List<Timeline> timeline;
        private String emotionFeedback;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
        public static class Timeline {
            private String time;
            private String momentEmotion;
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Frequency {
        private Integer parentFrequency;
        private Integer kidFrequency;
        private String frequencyFeedback;
    }
}
