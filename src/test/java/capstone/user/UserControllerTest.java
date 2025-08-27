package capstone.user;

import capstone.user.controller.UserController;
import capstone.user.dto.*;
import capstone.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@Import(UserControllerTest.MockConfig.class)
class UserControllerTest {

    @TestConfiguration
    static class MockConfig {
        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserService userService; // ↑ 위에서 등록한 Mockito mock이 주입됨

    @Test
    @DisplayName("회원가입 성공하면 201과 UserResponse 반환")
    void register_success() throws Exception {
        // given
        UserRegisterRequest req = UserRegisterRequest.builder()
                .email("parent@example.com")
                .password("MySecurePw123!")
                .parentNickname("모담")
                .goal("공감 대화 늘리기")
                .worry("감정 조절")
                .personalInformationAgree(1)
                .kids(List.of(
                        UserRegisterRequest.KidRequest.builder()
                                .kidsId(1).nickname("도담").age(5).tendency("활발").note("낯가림 없음").build()
                ))
                .build();

        UserResponse mockRes = UserResponse.builder()
                .userId(1L)
                .email("parent@example.com")
                .parentNickname("모담")
                .goal("공감 대화 늘리기")
                .worry("감정 조절")
                .personalInformationAgree(1)
                .kids(List.of(
                        UserResponse.KidResponse.builder()
                                .kidsId(10L).nickname("도담").age(5).tendency("활발").note("낯가림 없음").build()
                ))
                .build();

        Mockito.when(userService.register(any(UserRegisterRequest.class))).thenReturn(mockRes);

        // when & then
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("parent@example.com"))
                .andExpect(jsonPath("$.kids[0].nickname").value("도담"));
    }

    @Test
    @DisplayName("로그인 성공하면 200과 LoginResponse 반환")
    void login_success() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("parent@example.com")
                .password("MySecurePw123!")
                .build();

        LoginResponse res = LoginResponse.builder()
                .success(true)
                .message("로그인 성공")
                .userId(1L)
                .parentNickname("모담")
                .email("parent@example.com")
                .token(UUID.randomUUID().toString())
                .build();

        Mockito.when(userService.login(any(LoginRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.parentNickname").value("모담"));
    }

    @Test
    @DisplayName("로그인 실패하면 401 반환")
    void login_fail() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("parent@example.com")
                .password("wrong")
                .build();

        LoginResponse res = LoginResponse.builder()
                .success(false)
                .message("비밀번호가 올바르지 않습니다.")
                .build();

        Mockito.when(userService.login(any(LoginRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("사용자 조회 200")
    void getUser() throws Exception {
        UserResponse mockRes = UserResponse.builder()
                .userId(1L)
                .email("parent@example.com")
                .parentNickname("모담")
                .personalInformationAgree(1)
                .kids(List.of())
                .build();

        Mockito.when(userService.getUser(1L)).thenReturn(mockRes);

        mockMvc.perform(get("/api/users/{userId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("parent@example.com"));
    }
}
