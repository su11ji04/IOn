package capstone.chatbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter
@Setter
@ConfigurationProperties(prefix = "chatbot.python")
public class ChatbotPythonProperties {
    /** 예: http://localhost:8082 */
    private String baseUrl;
    /** 연결 타임아웃(ms) */
    private Integer connectTimeoutMs = 5000;
    /** 읽기 타임아웃(ms) */
    private Integer readTimeoutMs = 60000;
}
