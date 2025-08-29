package capstone.config;

import capstone.chatbot.config.ChatbotPythonProperties;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Configuration
@RequiredArgsConstructor                                           // ✅ props 생성자 주입
@EnableConfigurationProperties(ChatbotPythonProperties.class)
public class WebClientConfig {

    private final ChatbotPythonProperties props;
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

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


    @Bean(name = "chatbotWebClient")
    public WebClient chatbotWebClient() {
        HttpClient http = HttpClient.create()
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS));
                });

        String base = props.getBaseUrl();
        log.info("[CHATBOT] Using baseUrl={}", props.getBaseUrl());
        if (base == null || !(base.startsWith("http://") || base.startsWith("https://"))) {
            throw new IllegalArgumentException("chatbot.python.base-url must start with http:// or https:// : " + base);
        }

        return WebClient.builder()
                .baseUrl(base)   // ✅ props에서 읽어오기
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeaders(h -> h.setContentType(MediaType.APPLICATION_JSON))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }


}
