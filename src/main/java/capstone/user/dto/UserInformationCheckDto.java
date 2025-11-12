package capstone.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserInformationCheckDto {
    private int id;

    @Email @NotBlank
    private String email;

    @NotBlank
    private String parentName;
    @NotBlank
    private String parentNickname;

    @JsonProperty("kidsId")
    private Integer kidsId;

    @NotBlank
    @JsonProperty("kidsNickname")
    private String kidsNickname;
    @NotNull
    @JsonProperty("kidsAge")
    private Integer kidsAge;
    @JsonProperty("kidsTendency")
    private String kidsTendency;
    @JsonProperty("kidsNote")
    private String kidsNote;

    private String goal;
    private String worry;
    @NotNull
    private Integer personalInformationAgree;
}
