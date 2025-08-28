package capstone.chatbot.controller;

import capstone.chatbot.dto.ChatAnswerResponse;
import capstone.chatbot.dto.ChatAskRequest;
import capstone.chatbot.service.ChatbotPythonClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping
public class ChatbotController {

    private final ChatbotPythonClient chatbotPythonClient;

    @PostMapping(path = "/api/chatbot/ask",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ChatAnswerResponse> ask(@Valid @RequestBody ChatAskRequest req) {
        // ✅ 질문 수신 로그
        log.info("[ChatbotController] /api/chatbot/ask called. question='{}'", req.getQuestion());

        ChatAnswerResponse res = chatbotPythonClient.ask(req);

        // ✅ 응답 요약 로그
        log.info("[ChatbotController] response received. usedUserId={}, answerPreview={}",
                res.getUsedUserId(),
                (res.getAnswer() != null && res.getAnswer().length() > 80)
                        ? res.getAnswer().substring(0, 80) + "..."
                        : res.getAnswer());

        return ResponseEntity.ok(res);
    }
}
