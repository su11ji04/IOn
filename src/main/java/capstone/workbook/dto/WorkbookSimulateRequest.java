package capstone.workbook.dto;

import lombok.*;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookSimulateRequest {
    private String topic;
    private String userId; // ex) "u004"
    private Map<String, Object> user;
}
