package capstone.chatbot.sevice;

import capstone.chatbot.dto.AnswerDto;
import capstone.chatbot.dto.ChatbotListDto;
import capstone.chatbot.dto.QuestionAnswerDto;
import capstone.chatbot.dto.QuestionDto;
import capstone.chatbot.entity.Chat;
import capstone.chatbot.entity.QuestionAndAnswer;
import capstone.chatbot.repository.ChatRepository;
import capstone.home.entity.UserProfile;
import capstone.home.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotPythonClient chatbotPythonClient;
    private final ChatRepository chatRepository;
    private final UserProfileRepository userProfileRepository;

    //질문 -> 답변
    @Transactional
    public AnswerDto getAnswer(int userId, QuestionDto questionDto) {
        String nowQuestion = questionDto.getQuestion();
        if (nowQuestion == null || nowQuestion.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }

        // 세션 생성 or 기존 세션 조회
        Chat session;
        if (questionDto.getSessionId() == null) {
            session = Chat.builder()
                    .userId(userId)
                    .build();
            session = chatRepository.save(session);
        } else {
            session = chatRepository.findBySessionIdAndUserId(
                            questionDto.getSessionId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("invalid session id"));
            if (session.isClosed()) {
                throw new IllegalStateException("this chat session is already closed");
            }
        }

        // 질문 저장
        QuestionAndAnswer qa = QuestionAndAnswer.builder()
                .question(nowQuestion)
                .build();
        session.getQuestionAndAnswers().add(qa);

        // Question limit
        if (session.getQuestionAndAnswers().size() > 10) {
            String limitMessage =
                    "정확한 답변을 위해 하나의 세션에 10개까지의 질문만 가능합니다. "
                            + "더 많은 조언을 원하시면 새로 채팅을 시작해주세요.";

            qa.setAnswer(limitMessage);
            chatRepository.save(session);

            return AnswerDto.builder()
                    .sessionId(session.getSessionId())
                    .answer(limitMessage)
                    .build();
        }

        // Python 전달 parameter 정리
        List<QuestionAnswerDto> history = session.getQuestionAndAnswers().stream()
                .map(x -> QuestionAnswerDto.builder()
                        .question(x.getQuestion())
                        .answer(x.getAnswer())
                        .build())
                .toList();

        // Python 서버 호출
        AnswerDto bot = chatbotPythonClient.ask(
                userId,
                session.getSessionId(),
                history
        );
        String answer = (bot != null ? bot.getAnswer() : null);

        // 답변 저장
        qa.setAnswer(answer);

        // 세션 저장
        chatRepository.save(session);

        // user profile 관련 처리
        UserProfile p = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "유저 프로필이 존재하지 않습니다. userId=" + userId
                ));
        p.setUsedChatbotOnce(1);

        // null-safe 챗봇 사용 빈도 증가
        int cFrequency = Optional.ofNullable(p.getChatBotFrequency()).orElse(0) + 1;
        p.setChatBotFrequency(cFrequency);

        return AnswerDto.builder()
                .sessionId(session.getSessionId())
                .answer(answer)
                .build();
    }

    // 채팅 기록 조회
    @Transactional(readOnly = true)
    public Page<ChatbotListDto> list(int userId, Pageable pageable) {
        Page<Chat> page = chatRepository
                .findByUserIdOrderBySessionIdDesc(userId, pageable);

        return page.map(session ->
                ChatbotListDto.builder()
                        .sessionId(session.getSessionId())
                        .questions(
                                session.getQuestionAndAnswers().stream()
                                        .map(qa -> QuestionAnswerDto.builder()
                                                .question(qa.getQuestion())
                                                .answer(qa.getAnswer())
                                                .build())
                                        .toList()
                        )
                        .build()
        );
    }

    // 세션 종료
    @Transactional
    public void closeSession(int userId, int sessionId) {
        Chat session = chatRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("invalid session id"));

        UserProfile p = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저 프로필이 존재하지 않습니다."));
        p.setUsedChatbotOnce(1);
        int cFrequency = p.getChatBotFrequency() + 1;
        p.setWorkBookFrequency(cFrequency);
        int nowPoints = p.getPoints();
        p.setPoints(nowPoints+4);

        if (!session.isClosed()) {
            session.setClosed(true);
        }
    }
}
