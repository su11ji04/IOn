package capstone.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AuthRequestDto {
    @Email @NotBlank
    private String email;
    @NotBlank
    private String password;
}