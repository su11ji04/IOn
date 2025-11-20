package capstone.chatbot.controller;

import capstone.chatbot.dto.AnswerDto;
import capstone.chatbot.dto.ChatbotListDto;
import capstone.chatbot.dto.PagedListResponse;
import capstone.chatbot.dto.QuestionDto;
import capstone.chatbot.sevice.ChatbotService;
import capstone.voicereport.error.VoiceReportException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    // 질문 -> 답변
    @PostMapping
    public ResponseEntity<AnswerDto> createChatbotAnswer(
            HttpSession session,
            @RequestBody QuestionDto question
    ) {
        Integer sessionId = (Integer) session.getAttribute("userId");
        if (sessionId == null) throw VoiceReportException.loginRequired();

        AnswerDto answer = chatbotService.getAnswer(sessionId, question);
        return ResponseEntity.ok(answer);
    }

    // 채팅 종료
    @PostMapping("/{chatSessionId}/close")
    public ResponseEntity<Void> closeSession(
            HttpSession session,
            @PathVariable("chatSessionId") int chatSessionId
    ) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) throw VoiceReportException.loginRequired();

        chatbotService.closeSession(userId, chatSessionId);
        return ResponseEntity.ok().build();
    }

    // 채팅 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<List<ChatbotListDto>> list(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) throw VoiceReportException.loginRequired();

        int page = 0;
        int size = 10;

        Page<ChatbotListDto> p = chatbotService.list(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(p.getContent());  // <-- 여기!
    }
}
