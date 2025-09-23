// src/main/java/capstone/chatbot/controller/ChatbotController.java
package capstone.chatbot.controller;

import capstone.chatbot.dto.Chat;
import capstone.chatbot.dto.ChatAnswer;
import capstone.chatbot.dto.ChatQuestion;
import capstone.chatbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {
    private final ChatService chatService;

    @PostMapping(value = "/ask", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatAnswer ask(@RequestBody ChatQuestion req) {
        return chatService.askOne(req);
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Chat>> list(
            @RequestParam(name = "userId", defaultValue = "u001") String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        log.info("GET /api/chat/list userId={}, page={}, size={}", userId, page, size);
        return ResponseEntity.ok(chatService.list(userId, page, size));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Chat> get(@PathVariable("id") String id) {
        log.info("GET /api/chat/{}", id);
        return ResponseEntity.ok(chatService.get(id));
    }
}
