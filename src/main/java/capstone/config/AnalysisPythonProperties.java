package capstone.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "analysis.python")
public class AnalysisPythonProperties {
    private String baseUrl;
    private Integer connectTimeoutMs = 50000000;
    private Integer readTimeoutMs = 40000000;
}
