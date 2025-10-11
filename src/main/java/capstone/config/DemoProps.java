package capstone.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@Profile("demo")
@ConfigurationProperties(prefix = "demo")
public class DemoProps {
    private boolean enabled;
    private String userId; // u001
}