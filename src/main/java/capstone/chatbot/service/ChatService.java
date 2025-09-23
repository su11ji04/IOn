// src/main/java/capstone/chatbot/service/ChatService.java
package capstone.chatbot.service;

import capstone.chatbot.dto.Chat;
import capstone.chatbot.dto.ChatAnswer;
import capstone.chatbot.dto.ChatQuestion;
import capstone.chatbot.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatbotPythonClient pythonClient;
    private final ChatRepository chatRepository;

    public ChatAnswer askOne(ChatQuestion req) {
        // chatQuestionId 없으면 서버가 생성
        String chatQuestionId = (req.getChatQuestionId() == null || req.getChatQuestionId().isBlank())
                ? UUID.randomUUID().toString()
                : req.getChatQuestionId();

        String userId = (req.getUserId() == null || req.getUserId().isBlank()) ? "u001" : req.getUserId();
        // req 객체에 반영(아래 pythonClient로 넘길 때 null 방지)
        req.setUserId(userId);

        // 1) 파이썬 호출
        PythonAnswer pa = pythonClient.ask(req);

        // 2) chatAnswerId 생성
        String chatAnswerId = UUID.randomUUID().toString();

        // 3) DB 저장 (NOT NULL 필드 모두 채우기)
        Chat saved = chatRepository.save(
                Chat.builder()
                        .userId(req.getUserId())
                        .question(req.getQuestion())
                        .answer(pa.answer())
                        .chatQuestionId(chatQuestionId)
                        .chatAnswerId(chatAnswerId)
                        .build()
        );

        // 4) 응답 DTO
        return ChatAnswer.builder()
                .chatAnswerId(chatAnswerId)
                .chatQuestionId(chatQuestionId)
                .chatId(saved.getId())
                .answer(pa.answer())
                .build();
    }

    public Page<Chat> list(String userId, int page, int size) {
        // Repository 시그니처에 맞게 userId 기준으로 조회
        return chatRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    public Chat get(String id) {
        return chatRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found: " + id));
    }
}
