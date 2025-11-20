package capstone.chatbot.repository;

import capstone.chatbot.entity.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Integer> {
    Page<Chat> findByUserIdOrderBySessionIdDesc(int userId, Pageable pageable);
    Optional<Chat> findBySessionIdAndUserId(int sessionId, int userId);
}
