// capstone/chatbot/service/ChatbotPythonClient.java
package capstone.chatbot.service;

import capstone.chatbot.dto.ChatAnswerResponse;
import capstone.chatbot.dto.ChatAskRequest;
import capstone.support.userprofile.UserProfile;
import capstone.support.userprofile.UserProfileLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ChatbotPythonClient {

    private final WebClient chatbotWebClient;
    private final UserProfileLoader userProfileLoader;
    private static final String FIXED_USER_ID = "u001";

    public ChatbotPythonClient(
            @Qualifier("chatbotWebClient") WebClient chatbotWebClient,
            UserProfileLoader userProfileLoader
    ) {
        this.chatbotWebClient = chatbotWebClient;
        this.userProfileLoader = userProfileLoader;
    }

    public ChatAnswerResponse ask(ChatAskRequest req) {
        // 1) CSV에서 u001 로드
        var opt = userProfileLoader.find(FIXED_USER_ID);
        if (opt.isEmpty()) {
            log.error("[ChatbotPythonClient] UserProfile not found for userId={}", FIXED_USER_ID);
            throw new IllegalArgumentException("User not found: " + FIXED_USER_ID);
        }
        UserProfile up = opt.get();

        // ✅ CSV 로드 성공 로그 (Lombok @Value면 toString 안전)
        log.info("[ChatbotPythonClient] loaded user profile from CSV: {}", up);

        // 2) 파이썬 user 매핑
        Object childAge = up.getChildAge();
        try {
            if (up.getChildAge() != null) childAge = Integer.valueOf(up.getChildAge().trim());
        } catch (NumberFormatException ignore) { /* keep string */ }

        Map<String, Object> user = new HashMap<>();
        user.put("child_age", childAge);
        user.put("parenting_style", up.getParentingStyle());
        user.put("parenting_goal", up.getParentingGoal());
        user.put("child_traits", up.getChildTraits());
        user.put("preferred_tone", up.getPreferredTone());
        user.put("language", up.getLanguage());
        user.put("health_issues", up.getHealthIssues());

        // 3) 최종 payload 구성
        Map<String, Object> payload = new HashMap<>();
        payload.put("question", req.getQuestion());
        payload.put("user_id", up.getUserId());
        payload.put("user", user);

        // ✅ 전송 직전 요약 로그 (질문, user_id, user 요약)
        log.info("[ChatbotPythonClient] sending to Python: user_id={}, question='{}'",
                up.getUserId(), req.getQuestion());
        log.debug("[ChatbotPythonClient] payload.user={}", user);

        try {
            ChatAnswerResponse res = chatbotWebClient.post()
                    .uri("/chat/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(ChatAnswerResponse.class)
                    .block();

            // ✅ 응답 수신 로그
            if (res != null) {
                log.info("[ChatbotPythonClient] python responded. usedUserId={}, answer.len={}",
                        res.getUsedUserId(), res.getAnswer() == null ? 0 : res.getAnswer().length());
                log.debug("[ChatbotPythonClient] python response meta={}", res.getMeta());
            } else {
                log.warn("[ChatbotPythonClient] python response mapped to null");
            }
            return res;
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("[ChatbotPythonClient] HTTP {} {} => {}", e.getRawStatusCode(), e.getStatusText(), body);
            throw e;
        }
    }
}
