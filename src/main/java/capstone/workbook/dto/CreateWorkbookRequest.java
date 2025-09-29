package capstone.workbook.dto;

import lombok.*;
import java.util.Map;


@Getter @Setter
public class CreateWorkbookRequest {
    private String topic;
    private Map<String, Object> user;  // user profile
    private String userId;
}

