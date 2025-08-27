package capstone.voicereport.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalysisReportDto {
    private String subTitle;
    private String day;
    private String conversationSummary;
    private Integer length;
    private String overallFeedback;

    private Frequency frequency;
    private Expression expression;
    private Emotion emotion;

    private String kidAttitude;
    private List<ChangeProposal> changeProposal;

    private String pattern;
    private String strength;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Frequency {
        private Integer parentFrequency;
        private Integer kidFrequency;
        private String frequencyFeedback;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Expression {
        private String parentExpression;
        private String kidExpression;
        private String parentConditions;
        private String kidConditions;
        private String expressionFeedback;
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
    public static class ChangeProposal {
        private String existingExpression;
        private String proposalExpression;
    }
}
