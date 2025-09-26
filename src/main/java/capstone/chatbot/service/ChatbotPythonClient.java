package capstone.chatbot.service;

import capstone.chatbot.dto.ChatQuestion;
import capstone.support.userprofile.UserProfile;
import capstone.support.userprofile.UserProfileLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class ChatbotPythonClient {

    private final WebClient chatbotWebClient;
    private final UserProfileLoader userProfileLoader;

    public ChatbotPythonClient(
            @Qualifier("chatbotWebClient") WebClient chatbotWebClient,
            UserProfileLoader userProfileLoader
    ) {
        this.chatbotWebClient = chatbotWebClient;
        this.userProfileLoader = userProfileLoader;
    }

    public PythonAnswer ask(ChatQuestion req) {
        // 1) userId 보정
        String userId = (req.getUserId() == null || req.getUserId().isBlank()) ? "u001" : req.getUserId();

        // 2) CSV 프로필 로드 + 로그
        log.info("[UserProfileLoader] find userId={}", userId);
        Optional<UserProfile> opt = userProfileLoader.find(userId);
        opt.ifPresentOrElse(
                p -> log.info("[UserProfileLoader] loaded profile for {} => {}", userId, p),
                () -> log.warn("[UserProfileLoader] no profile found for {}", userId)
        );

        // 3) payload.user 구성
        Map<String, Object> user = opt.map(userProfileLoader::toPythonMap)
                .map(HashMap::new) // mutable 보장
                .orElseGet(HashMap::new);

        // 4) 최종 payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("question", req.getQuestion());
        payload.put("user", user);

        log.info("[ChatbotPythonClient] final payload to python = {}", payload);

        try {
            Map<String, Object> resp = chatbotWebClient.post()
                    .uri("/chat/ask") // FastAPI 라우트 확인
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            String answer = null;
            String usedUserId = userId;
            Map<String, Object> meta = Map.of();

            if (resp != null) {
                Object a = resp.get("answer");
                if (a != null) answer = String.valueOf(a);

                Object u = resp.get("used_user_id");
                if (u != null) usedUserId = String.valueOf(u);

                Object m = resp.get("meta");
                if (m instanceof Map) {
                    //noinspection unchecked
                    meta = (Map<String, Object>) m;
                }
            }

            log.info("[ChatbotPythonClient] python responded. usedUserId={}, answer.len={}",
                    usedUserId, answer == null ? 0 : answer.length());

            return new PythonAnswer(answer, usedUserId, meta);

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("[ChatbotPythonClient] HTTP {} {} => {}", e.getRawStatusCode(), e.getStatusText(), body);
            throw e;
        }
    }
}
