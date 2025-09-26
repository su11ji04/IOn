package capstone.workbook.service;

import lombok.*;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkbookSimulateRequest {
    private String topic;
    private String userId;
    private Map<String, Object> user;
}
