package capstone.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRegisterRequest {

    @Email @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @NotBlank
    private String parentNickname;

    private String goal;
    private String worry;

    @NotNull
    private Integer personalInformationAgree;

    @NotNull
    @Size(min = 0)
    private List<KidRequest> kids;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KidRequest {
        private Integer kidsId;
        @NotBlank
        private String nickname;
        @NotNull
        private Integer age;
        private String tendency;
        private String note;
    }
}
