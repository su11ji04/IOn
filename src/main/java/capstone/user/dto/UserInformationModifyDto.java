package capstone.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInformationModifyDto {

    @NotBlank
    private String password;

    private String userImage;
    @NotBlank
    private String parentName;
    @NotBlank
    private String parentNickname;

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
    private Integer personalInformationAgree; // 1/0

    private List<UserInformationModifyDto.PropensityTestDto> propensityTest;
    @Getter
    @Setter
    public static class PropensityTestDto {
        private int propensityTestId;
        private int propensityTestScore;
    }
}
