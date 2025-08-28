package capstone.config;

import capstone.support.userprofile.UserProfileProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({UserProfileProperties.class})
public class AppPropertiesConfig {}
