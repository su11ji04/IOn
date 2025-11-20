package capstone.config;

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
        AnalysisPythonProperties.class,
        WorkbookPythonProperties.class,
        ChatbotPythonProperties.class
})
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);


    @Bean(name = "pythonAnalyzerWebClient")
    public WebClient pythonAnalyzerWebClient(AnalysisPythonProperties props) {
        String baseUrl = props.getBaseUrl();
        int connectTimeoutMs = props.getConnectTimeoutMs();
        int readTimeoutMs = props.getReadTimeoutMs();

        log.info("[PYTHON-ANALYZER] Using baseUrl={} (connect={}ms, read={}ms)",
                baseUrl, connectTimeoutMs, readTimeoutMs);

        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(256 * 1024 * 1024))
                        .build())
                .build();
    }


    // === Chatbot ===
    @Bean(name = "chatbotWebClient")
    public WebClient chatbotWebClient(ChatbotPythonProperties props) {
        log.info("[CHATBOT] baseUrl={} (connect={}ms, read={}ms)",
                props.getBaseUrl(), props.getConnectTimeoutMs(), props.getReadTimeoutMs());

        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()));

        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

    private static <T> T firstNonNull(T a, T b) { return (a != null) ? a : b; }


    @Bean(name = "workbookCreateWebClient")
    public WebClient workbookCreateWebClient(WorkbookPythonProperties props) {
        log.info("[WORKBOOK-CREATE] baseUrl={} (connect={}ms, read={}ms)",
                props.getBaseUrlCreate(), props.getConnectTimeoutMs(), props.getReadTimeoutMs());
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()));

        return WebClient.builder()
                .baseUrl(props.getBaseUrlCreate())    // ★ 9096
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

    @Bean(name = "workbookFeedbackWebClient")
    public WebClient workbookFeedbackWebClient(WorkbookPythonProperties props) {
        log.info("[WORKBOOK-FEEDBACK] baseUrl={} (connect={}ms, read={}ms)",
                props.getBaseUrlFeedback(), props.getConnectTimeoutMs(), props.getReadTimeoutMs());

        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()));

        return WebClient.builder()
                .baseUrl(props.getBaseUrlFeedback())   // ★ 9098
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

}
