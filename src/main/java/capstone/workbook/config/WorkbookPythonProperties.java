package capstone.workbook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "workbook.python")
public class WorkbookPythonProperties {
    private String baseUrl;
    private Integer readTimeoutMs = 120000;
}
