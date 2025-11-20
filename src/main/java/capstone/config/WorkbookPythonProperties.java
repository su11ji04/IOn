package capstone.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "workbook.python")
public class WorkbookPythonProperties {

    // 생성용 서버 (9096)
    private String baseUrlCreate;

    // 피드백용 서버 (9098)
    private String baseUrlFeedback;

    private int connectTimeoutMs = 500000000;
    private int readTimeoutMs = 600000000;
}