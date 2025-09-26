package capstone.config;

import capstone.chatbot.config.ChatbotPythonProperties;
import capstone.support.userprofile.UserProfileProperties;
import capstone.workbook.config.WorkbookPythonProperties;   // ✅ 추가
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({
        ChatbotPythonProperties.class,
        WorkbookPythonProperties.class,
        UserProfileProperties.class
})
public class WebClientConfig {

    private final ChatbotPythonProperties props;
    private final WorkbookPythonProperties workbookProps;

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean(name = "pythonAnalyzerWebClient")
    public WebClient pythonAnalyzerWebClient() {
        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5));

        return WebClient.builder()
                .baseUrl("http://127.0.0.1:8081")
                .clientConnector(new ReactorClientHttpConnector(http))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean(name = "chatbotWebClient")
    public WebClient chatbotWebClient() {
        String base = props.getBaseUrl(); // application.yml 의 chatbot.python.base-url
        log.info("[CHATBOT] Using baseUrl={}", base);
        if (base == null || !(base.startsWith("http://") || base.startsWith("https://"))) {
            throw new IllegalArgumentException("chatbot.python.base-url must start with http:// or https:// : " + base);
        }

        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS));
                });

        return WebClient.builder()
                .baseUrl(base)
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeaders(h -> h.setContentType(MediaType.APPLICATION_JSON))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

    @Bean(name = "workbookWebClient")
    public WebClient workbookWebClient() {
        String base = workbookProps.getBaseUrl();
        log.info("[WORKBOOK] Using baseUrl={}", base);

        HttpClient http = HttpClient.create()
                .wiretap("reactor.netty.http.client",
                        io.netty.handler.logging.LogLevel.DEBUG,
                        reactor.netty.transport.logging.AdvancedByteBufFormat.TEXTUAL) // ★ 추가
                .responseTimeout(Duration.ofMillis(workbookProps.getReadTimeoutMs()));

        return WebClient.builder()
                .baseUrl(base)
                .clientConnector(new ReactorClientHttpConnector(http))
                .defaultHeaders(h -> h.setContentType(MediaType.APPLICATION_JSON))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
}
