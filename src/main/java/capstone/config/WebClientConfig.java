package capstone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;


@Configuration
public class WebClientConfig {

    @Bean(name = "pythonAnalyzerWebClient")
    public WebClient pythonAnalyzerWebClient() {
        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5)); // 필요에 맞게 늘림

        return WebClient.builder()
                .baseUrl("http://127.0.0.1:8081")
                .clientConnector(new ReactorClientHttpConnector(http))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

}
