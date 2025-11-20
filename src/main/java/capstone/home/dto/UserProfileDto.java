package capstone.home.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private String userImage; // 사용자 사진

    private Integer level; // 사용자 레벨
    private String parentNickname; // 부모 닉네임
    private Integer points; // 포인트

    private Integer streakDay; // 연속 접속일
    private String phrase; // 명언

    private Integer monthFrequency; // 이번달 접속일
    private Integer voicereportFrequency; // 이번달 보이스리포트 이용 횟수
    private Integer chatBotFrequency; // 이번달 챗봇 이용 횟수
    private Integer workBookFrequency; // 이번달 진행 워크북 수 (ex. 03 Lesson)

    private String message; // 제안 메시지

    private List<Reward> reward;

    @Getter
    @Setter
    public static class Reward {
        private Integer rewardId; // 리워드 ID
        private String earnedAt; // 리워드 내용
    }
}
