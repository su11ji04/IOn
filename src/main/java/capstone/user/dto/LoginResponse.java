package capstone.user.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private boolean success;
    private String message;
    private Long userId;
    private String parentNickname;
    private String email;
    private String token;
}


