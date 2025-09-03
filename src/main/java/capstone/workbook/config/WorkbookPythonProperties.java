package capstone.workbook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "workbook.python")
public class WorkbookPythonProperties {
    private String baseUrl;          // e.g. http://127.0.0.1:8083
    private Integer readTimeoutMs = 120000; // 120s
}
