package capstone.user.service;

import capstone.user.dto.*;
import capstone.user.entity.Kid;
import capstone.user.entity.User;
import capstone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public UserResponse register(UserRegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .parentNickname(req.getParentNickname())
                .goal(req.getGoal())
                .worry(req.getWorry())
                .personalInformationAgree(req.getPersonalInformationAgree())
                .build();

        req.getKids().forEach(k -> {
            Kid kid = Kid.builder()
                    .nickname(k.getNickname())
                    .age(k.getAge())
                    .tendency(k.getTendency())
                    .note(k.getNote())
                    .build();
            user.addKid(kid);
        });

        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return LoginResponse.builder()
                    .success(false)
                    .message("비밀번호가 올바르지 않습니다.")
                    .build();
        }

        // 실제 환경에서는 JWT 발급
        String demoToken = UUID.randomUUID().toString();

        return LoginResponse.builder()
                .success(true)
                .message("로그인 성공")
                .userId(user.getId())
                .parentNickname(user.getParentNickname())
                .email(user.getEmail())
                .token(demoToken)
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .parentNickname(user.getParentNickname())
                .goal(user.getGoal())
                .worry(user.getWorry())
                .personalInformationAgree(user.getPersonalInformationAgree())
                .kids(user.getKids().stream()
                        .map(k -> UserResponse.KidResponse.builder()
                                .kidsId(k.getId())
                                .nickname(k.getNickname())
                                .age(k.getAge())
                                .tendency(k.getTendency())
                                .note(k.getNote())
                                .build())
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<UserResponse> getUserByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toUserResponse);
    }
}
