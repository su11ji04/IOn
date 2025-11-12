package capstone.home.service;

import capstone.home.dto.UserProfileDto;
import capstone.home.entity.UserProfile;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final capstone.home.repository.UserProfileRepository userProfileRepository;

    private UserProfile load(int userId) {
        return userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 프로필이 존재하지 않습니다."));
    }

    // 월간 카운트( activity/chatbot/workbook )가 새로운 달이면 0으로 초기화
    private void resetMonthlyIfNeeded(UserProfile p, LocalDate todaySeoul) {
        LocalDate anchor = p.getMonthlyAnchor();
        YearMonth currentYm = YearMonth.from(todaySeoul);

        if (anchor == null || !YearMonth.from(anchor).equals(currentYm)) {
            p.setActivityFrequency(0);
            p.setChatBotFrequency(0);
            p.setWorkBookFrequency(0);
            p.setMonthlyAnchor(currentYm.atDay(1));
        }
    }

    // 포인트 100마다 레벨업
    private void addPointsAndMaybeLevelUp(UserProfile p, int add) {
        int curPoints = p.getPoints() == null ? 0 : p.getPoints();
        int newPoints = curPoints + add;
        int curLevel = p.getLevel() == null ? 0 : p.getLevel();

        if (newPoints >= 100) {
            int levelUp = newPoints / 100;
            p.setLevel(curLevel + levelUp);
            p.setPoints(newPoints % 100); // 정확히 100이면 0
        } else {
            p.setPoints(newPoints);
        }
    }

    private LocalDate todaySeoul() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    // ---------------- 액션 핸들러 ----------------
    @Transactional
    public UserProfileDto recordChatbotUse(int userId) {
        UserProfile p = load(userId);
        LocalDate today = todaySeoul();
        resetMonthlyIfNeeded(p, today);

        p.setChatBotFrequency((p.getChatBotFrequency() == null ? 0 : p.getChatBotFrequency()) + 1);
        addPointsAndMaybeLevelUp(p, 5);
        return toDto(p);
    }

    @Transactional
    public UserProfileDto recordVoiceReportComplete(int userId) {
        UserProfile p = load(userId);
        LocalDate today = todaySeoul();
        resetMonthlyIfNeeded(p, today);

        p.setActivityFrequency((p.getActivityFrequency() == null ? 0 : p.getActivityFrequency()) + 1);
        addPointsAndMaybeLevelUp(p, 5);
        return toDto(p);
    }

    @Transactional
    public UserProfileDto recordWorkbookLessonComplete(int userId) {
        UserProfile p = load(userId);
        LocalDate today = todaySeoul();
        resetMonthlyIfNeeded(p, today);

        p.setWorkBookFrequency((p.getWorkBookFrequency() == null ? 0 : p.getWorkBookFrequency()) + 1);
        addPointsAndMaybeLevelUp(p, 5);
        return toDto(p);
    }

    // ---------------- DTO 변환 ----------------
    public UserProfileDto getUserProfileContent(int userId) {
        return toDto(load(userId));
    }

    private UserProfileDto toDto(UserProfile profile) {
        return UserProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .level(profile.getLevel())
                .parentNickname(profile.getParentNickname())
                .points(profile.getPoints())
                .streakDay(profile.getStreakDay())
                .phrase(profile.getPhrase())
                .activityFrequency(profile.getActivityFrequency())
                .chatBotFrequency(profile.getChatBotFrequency())
                .workBookFrequency(profile.getWorkBookFrequency())
                .message(profile.getMessage())
                .reward(profile.getReward() == null ? null :
                        profile.getReward().stream().map(r -> {
                            UserProfileDto.Reward dto = new UserProfileDto.Reward();
                            dto.setRewardId(r.getRewardId());
                            dto.setEarnedAt(r.getEarnedAt());
                            return dto;
                        }).collect(Collectors.toList()))
                .build();
    }
}
