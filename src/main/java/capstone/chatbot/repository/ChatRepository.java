package capstone.chatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import capstone.chatbot.dto.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRepository extends JpaRepository<Chat, String> {
    Page<Chat> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}

