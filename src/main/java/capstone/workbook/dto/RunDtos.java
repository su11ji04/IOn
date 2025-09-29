package capstone.workbook.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

public class RunDtos {

    @Getter @Setter
    public static class StartResponse {
        private Long runId;
        private Map<String,Object> mcq;     // MCQ 문제
    }

    @Getter @Setter
    public static class McqAnswerRequest {
        private String answer;              // MCQ 문제 ANSWER
    }

    @Getter @Setter
    public static class WritingAnswerRequest {
        private String text;
    }

    @Getter @Setter
    public static class SimNextRequest {
        private String parentReply;         // 시뮬레이션 응답 1
    }

    @Getter @Setter
    public static class SimNextResponse {
        private String aiLine;              // 아이 2번째 대사
        private boolean finished;           // 시뮬레이션 응답 2
    }

    @Getter @Setter
    public static class FeedbackResponse {
        private String overallComment;
        private List<String> tips;
    }
}
