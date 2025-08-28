package capstone.support.userprofile;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "userprofile")
public class UserProfileProperties {
    private String csvPath;
}
