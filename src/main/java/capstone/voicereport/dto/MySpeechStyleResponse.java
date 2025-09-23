package capstone.voicereport.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MySpeechStyleResponse {
    private Long userId;
    private int reportCount;
    private List<String> overallFeedbacks;
    private List<String> parentExpressions;
    private Map<String, Integer> topKeywords;
}
