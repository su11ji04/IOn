package capstone.workbook.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.Map;

public class RunDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StartResponse {
        private Long runId;
        private Map<String,Object> mcq;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class McqAnswerRequest {
        private String answer;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WritingQuestion {
        private String instruction;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class WritingAnswerRequest {
        private String text;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SimStartResponse {
        private String instruction;        // "아이와 숙제를 하기 싫다고 할 때 대화를 완성하세요."
        private String situation;          // "아이: 숙제 하기 싫어!"
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SimAnswerRequest {
        private String parentReply;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SimAnswerResponse {
        private boolean finished;
    }
}
