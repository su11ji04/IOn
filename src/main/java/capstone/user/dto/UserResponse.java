package capstone.user.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long userId;
    private String email;
    private String parentNickname;
    private String goal;
    private String worry;
    private Integer personalInformationAgree;
    private List<KidResponse> kids;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KidResponse {
        private Long kidsId;
        private String nickname;
        private Integer age;
        private String tendency;
        private String note;
    }
}
