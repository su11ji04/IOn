package capstone.config;

import capstone.support.userprofile.UserProfileProperties;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
        AnalysisPythonProperties.class
})
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

//    // === Analyzer (VoiceReport) ===
//    @Bean(name = "pythonAnalyzerWebClient")
//    public WebClient pythonAnalyzerWebClient(
//            @Value("${analysis.python.baseUrl:${analysis.python.base-url:http://127.0.0.1:8081}}") String baseUrl,
//            @Value("${analysis.python.connectTimeoutMs:5000}") int connectTimeoutMs,
//            @Value("${analysis.python.readTimeoutMs:600000}") int readTimeoutMs
//    ) {
//        log.info("[PYTHON-ANALYZER] Using baseUrl={}", baseUrl);
//
//        HttpClient http = HttpClient.create()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
//                .responseTimeout(Duration.ofMillis(readTimeoutMs))
//                .doOnConnected(conn -> {
//                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS));
//                    conn.addHandlerLast(new WriteTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS));
//                });
//
//        return WebClient.builder()
//                .baseUrl(baseUrl)
//                .clientConnector(new ReactorClientHttpConnector(http))
//                .defaultHeaders(h -> h.setContentType(MediaType.APPLICATION_JSON))
//                .exchangeStrategies(ExchangeStrategies.builder()
//                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
//                        .build())
//                .build();
//    }


    @Bean(name = "pythonAnalyzerWebClient")
    public WebClient pythonAnalyzerWebClient(AnalysisPythonProperties props) {
        String baseUrl = props.getBaseUrl();
        int connectTimeoutMs = props.getConnectTimeoutMs();
        int readTimeoutMs = props.getReadTimeoutMs(); // 여길 yml에서 30분(1_800_000ms)로

        log.info("[PYTHON-ANALYZER] Using baseUrl={} (connect={}ms, read={}ms)",
                baseUrl, connectTimeoutMs, readTimeoutMs);

        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                // 응답 헤더 받을 때까지 총 대기 시간(“진짜” 늘려야 하는 타임아웃)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        // 👇 이 두 줄은 “유휴 시간” 기준으로 끊어버리므로 제거하는 게 안전
        // .doOnConnected(conn -> {
        //     conn.addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS));
        //     conn.addHandlerLast(new WriteTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS));
        // });

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        // 응답 본문이 큰 게 아니라면 10MB 그대로도 충분. 넉넉히 주고 싶으면 256MB.
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(256 * 1024 * 1024))
                        .build())
                .build();
    }


//    // === Chatbot ===
//    @Bean(name = "chatbotWebClient")
//    public WebClient chatbotWebClient() {
//        String base = props.getBaseUrl();
//        log.info("[CHATBOT] Using baseUrl={}", base);
//        if (base == null || !(base.startsWith("http://") || base.startsWith("https://"))) {
//            throw new IllegalArgumentException("chatbot.python.base-url must start with http:// or https:// : " + base);
//        }
//
//        HttpClient http = HttpClient.create()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
//                .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
//                .doOnConnected(conn -> {
//                    conn.addHandlerLast(new ReadTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS));
//                    conn.addHandlerLast(new WriteTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS));
//                });
//
//        return WebClient.builder()
//                .baseUrl(base)
//                .clientConnector(new ReactorClientHttpConnector(http))
//                .defaultHeaders(h -> h.setContentType(MediaType.APPLICATION_JSON))
//                .exchangeStrategies(ExchangeStrategies.builder()
//                        .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
//                        .build())
//                .build();
//    }
//


    private static <T> T firstNonNull(T a, T b) { return (a != null) ? a : b; }
}
