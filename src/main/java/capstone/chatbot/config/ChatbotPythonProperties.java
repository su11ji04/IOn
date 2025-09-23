package capstone.chatbot.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter @Setter
@Validated
@ConfigurationProperties(prefix = "chatbot.python")
public class ChatbotPythonProperties {
    @NotBlank
    private String baseUrl;
    private Integer connectTimeoutMs = 5000;
    private Integer readTimeoutMs = 60000;
}