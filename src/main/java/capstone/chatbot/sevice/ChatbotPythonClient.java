package capstone.chatbot.sevice;

import capstone.chatbot.dto.AnswerDto;
import capstone.chatbot.dto.QuestionAnswerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatbotPythonClient {

    private final @Qualifier("chatbotWebClient") WebClient chatbotWebClient;

    public AnswerDto ask(int userId, Integer sessionId, List<QuestionAnswerDto> history) {
        if (history == null || history.isEmpty()) {
            throw new BadRequestException("questions history must not be empty");
        }

        try {
            return chatbotWebClient.post()
                    .uri("/api/chatbot")
                    .header("userId", String.valueOf(userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(
                            Map.of(
                                    "sessionId", sessionId,
                                    "questions", history
                            )
                    )
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, this::map4xx)
                    .onStatus(HttpStatusCode::is5xxServerError, this::map5xx)
                    .bodyToMono(AnswerDto.class)
                    .timeout(Duration.ofSeconds(300))
                    .onErrorMap(java.util.concurrent.TimeoutException.class,
                            e -> new ServerException("Chatbot timeout"))
                    .block();
        } catch (BadRequestException | ServerException e) {
            throw e;
        } catch (Exception e) {
            throw new ServerException("Chatbot call failed: " + e.getMessage());
        }
    }

    private Mono<? extends Throwable> map4xx(ClientResponse resp) {
        return resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new BadRequestException("[4xx] Chatbot: " + body));
    }

    private Mono<? extends Throwable> map5xx(ClientResponse resp) {
        return resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new ServerException("[5xx] Chatbot: " + body));
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String msg) { super(msg); }
    }

    public static class ServerException extends RuntimeException {
        public ServerException(String msg) { super(msg); }
    }
}

