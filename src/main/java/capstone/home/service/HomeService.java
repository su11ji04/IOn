package capstone.home.service;

import capstone.home.dto.UserProfileDto;
import capstone.home.entity.Phrase;
import capstone.home.entity.Reward;
import capstone.home.entity.UserProfile;
import capstone.home.repository.PhraseRepository;
import capstone.home.repository.UserProfileRepository;
import capstone.user.entity.User;
import capstone.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PhraseRepository phraseRepository;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private void addRewardIfNeeded(List<Reward> list, int id, String ts) {
        boolean exists = list.stream().anyMatch(r -> Objects.equals(r.getRewardId(), id));
        if (!exists) {
            list.add(Reward.builder()
                    .rewardId(id)
                    .earnedAt(ts)
                    .build());
        }
    }

    // Home Info Update
    @Transactional
    public void applyDailyUpdate(int userId) {
        UserProfile p = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 프로필이 존재하지 않습니다."));

        // user image
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));
        p.setUserImage(user.getUserImage());

        // points & level
        int nowPoints = Optional.ofNullable(p.getPoints()).orElse(0);
        int nowLevel  = Optional.ofNullable(p.getLevel()).orElse(0);
        if (nowPoints >= 100) {
            p.setPoints(nowPoints - 100);
            p.setLevel(nowLevel + 1);
        }

        // streakDay
        LocalDate today = LocalDate.now(KST);
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastLogin = p.getLastLoginDate();
        int currentStreak = Optional.ofNullable(p.getStreakDay()).orElse(0);
        if (lastLogin != null && lastLogin.equals(yesterday)) {
            p.setStreakDay(currentStreak + 1);
        } else if (lastLogin == null || !lastLogin.equals(today)) {
            p.setStreakDay(1);
        }
        p.setLastLoginDate(today);

        // phrase
        phraseRepository.findRandomOne()
                .map(Phrase::getPhraseContent)
                .ifPresent(p::setPhrase);

        // 이번달 접속일
        String nowTag = YearMonth.from(today).toString(); // "yyyy-MM"
        String lastTag = p.getLastLoginMonth();
        int monthFreq = Optional.ofNullable(p.getMonthFrequency()).orElse(0);
        if (lastTag == null || !lastTag.equals(nowTag)) {
            p.setMonthFrequency(1);
            p.setLastLoginMonth(nowTag);
        } else {
            if (lastLogin == null || !lastLogin.equals(today)) {  // <- lastLogin 사용!
                p.setMonthFrequency(monthFreq + 1);
            }
        }


        // message
        String[] msgs = {
                "보이스리포트를 이용해 대화 피드백을 받아볼까요?",
                "챗봇으로 육아 조언을 구해보세요!",
                "워크북으로 기초를 다지는건 어떨까요?"
        };
        p.setMessage(msgs[ThreadLocalRandom.current().nextInt(msgs.length)]);

        // reward
        List<Reward> rewards = Optional.ofNullable(p.getReward()).orElseGet(ArrayList::new);
        String nowStr = LocalDateTime.now(KST).format(TS);
        if (Optional.ofNullable(p.getLevel()).orElse(0) >= 50)  addRewardIfNeeded(rewards, 2, nowStr);
        if (Optional.ofNullable(p.getLevel()).orElse(0) >= 100) addRewardIfNeeded(rewards, 3, nowStr);
        if (Optional.ofNullable(p.getUsedVoiceReportOnce()).orElse(0) == 1
                || Optional.ofNullable(p.getVoicereportFrequency()).orElse(0) >= 1) addRewardIfNeeded(rewards, 4, nowStr);
        if (Optional.ofNullable(p.getFinishedWorkbookOnce()).orElse(0) == 1
                || Optional.ofNullable(p.getWorkBookFrequency()).orElse(0) >= 1) addRewardIfNeeded(rewards, 5, nowStr);
        if (Optional.ofNullable(p.getUsedChatbotOnce()).orElse(0) == 1
                || Optional.ofNullable(p.getChatBotFrequency()).orElse(0) >= 1) addRewardIfNeeded(rewards, 6, nowStr);
        p.setReward(rewards);

        userProfileRepository.save(p);
    }

    @Transactional
    public UserProfileDto getUserProfileContent(Integer userId) {
        // 기본 검증 & 업데이트
        if (userId == null) {
            throw new IllegalArgumentException("세션이 만료되었습니다. userId가 없습니다.");
        }
        applyDailyUpdate(userId);
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 프로필이 존재하지 않습니다."));

        return UserProfileDto.builder()
                .userImage(profile.getUserImage())
                .level(profile.getLevel())
                .parentNickname(profile.getParentNickname())
                .points(profile.getPoints())
                .streakDay(profile.getStreakDay())
                .phrase(profile.getPhrase())
                .monthFrequency(profile.getMonthFrequency())
                .voicereportFrequency(profile.getVoicereportFrequency())
                .chatBotFrequency(profile.getChatBotFrequency())
                .workBookFrequency(profile.getWorkBookFrequency())
                .message(profile.getMessage())
                .reward(profile.getReward() == null ? null :
                        profile.getReward().stream()
                                .map(r -> {
                                    UserProfileDto.Reward dto = new UserProfileDto.Reward();
                                    dto.setRewardId(r.getRewardId());
                                    dto.setEarnedAt(r.getEarnedAt());
                                    return dto;
                                })
                                .collect(Collectors.toList()))
                .build();
    }


}
