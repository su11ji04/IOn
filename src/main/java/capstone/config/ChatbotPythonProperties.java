package capstone.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "chatbot.python")
public class ChatbotPythonProperties {
    private String baseUrl;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 60000;
}
