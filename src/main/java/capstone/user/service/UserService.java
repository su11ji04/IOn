package capstone.user.service;

import capstone.common.service.FileStorageService;
import capstone.home.entity.Reward;
import capstone.home.entity.UserProfile;
import capstone.user.dto.*;
import capstone.user.entity.User;
import capstone.user.entity.PropensityTestResult;
import capstone.user.entity.TestListScore;
import capstone.user.entity.SubtypeScore;
import capstone.user.repository.PropensityTestRepository;
import capstone.user.repository.PropensityTestResultRepository;
import capstone.user.repository.UserRepository;
import capstone.workbook.service.WorkbookService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.ZoneId;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import capstone.home.repository.UserProfileRepository;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static capstone.user.error.UserException.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PropensityTestRepository propensityTestRepository;
    private final PropensityTestResultRepository propensityTestResultRepository;
    private final UserProfileRepository userProfileRepository;
    private final FileStorageService fileStorageService;
    private final WorkbookService workbookService;

    // 하위 타입: 문항 ID 목록
    private static final Map<String, List<Integer>> SUBTYPE_ITEMS = Map.of(
            "Warmth & Support", List.of(2, 8, 11, 16, 24),
            "Reasoning & Induction", List.of(4, 7, 14, 21, 26),
            "Democratic Participation", List.of(5, 13, 20, 27, 31),
            "Physical Coercion", List.of(3, 9, 17, 22),
            "Verbal Hostility", List.of(1, 12, 18, 30),
            "Non-Reasoning & Punitive", List.of(10, 15, 23, 28),
            "Indulgent Dimension", List.of(6, 19, 25, 29, 32)
    );

    // 상위 타입
    private static final String AUTHORITATIVE = "authoritative";
    private static final String AUTHORITARIAN = "authoritarian";
    private static final String PERMISSIVE = "permissive";

    private static final Map<String, List<String>> TYPE_TO_SUBTYPES = Map.of(
            AUTHORITATIVE, List.of("Warmth & Support", "Reasoning & Induction", "Democratic Participation"),
            AUTHORITARIAN, List.of("Physical Coercion", "Verbal Hostility", "Non-Reasoning & Punitive"),
            PERMISSIVE, List.of("Indulgent Dimension")
    );

    private static double mean(Map<String, Double> src, List<String> keys) {
        return keys.stream().mapToDouble(k -> {
            Double v = src.get(k);
            if (v == null) throw new IllegalArgumentException("서브타입 평균 누락: " + k);
            return v;
        }).average().orElse(Double.NaN);
    }

    // 유형검사 항목 보내기
    @Transactional(readOnly = true)
    public List<PropensityTestContentDto> getAllTests() {
        return propensityTestRepository.findAllAsContentDto();
    }

    // 회원가입
    @Transactional
    public UserIdDto register(UserRegisterDto req,  MultipartFile image) {
        // 기본 검증
        if (userRepository.existsByEmail(req.getEmail())) {
            throw emailDuplicate();
        }
        if (req.getPropensityTest() == null || req.getPropensityTest().isEmpty()) {
            throw propensityMissing();
        }
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.save(image);
        }

        // 점수 검증
        for (UserRegisterDto.PropensityTestDto dto : req.getPropensityTest()) {
            int score = dto.getPropensityTestScore();
            if (score < 1 || score > 6) {
                throw scoreOutOfRange(dto.getPropensityTestId());
            }
        }

        // 비밀번호 해시
        String hashed = passwordEncoder.encode(req.getPassword());

        // 사용자 생성/저장 (kidsId 고정 1)
        User user = User.builder()
                .email(req.getEmail())
                .password(hashed)
                .userImage(imageUrl)
                .parentName(req.getParentName())
                .parentNickname(req.getParentNickname())
                .kidsId(1)
                .kidsNickname(req.getKidsNickname())
                .kidsAge(req.getKidsAge())
                .kidsTendency(req.getKidsTendency())
                .kidsNote(req.getKidsNote())
                .goal(req.getGoal())
                .worry(req.getWorry())
                .personalInformationAgree(req.getPersonalInformationAgree())
                .nowChapter(1)
                .build();

        User saved = userRepository.save(user);

        // (id -> score) 맵 구성 - int 유지
        Map<Integer, Integer> scoreMap = new LinkedHashMap<>();
        for (UserRegisterDto.PropensityTestDto a : req.getPropensityTest()) {
            scoreMap.put(a.getPropensityTestId(), a.getPropensityTestScore());
        }

        // 서브타입 평균 계산
        Map<String, Double> subtypeAvg = new LinkedHashMap<>();
        List<SubtypeScore> subtypeEntities = new ArrayList<>();
        for (var entry : SUBTYPE_ITEMS.entrySet()) {
            String subtypeName = entry.getKey();
            List<Integer> items = entry.getValue();

            double avg = items.stream().mapToDouble(q -> {
                Integer s = scoreMap.get(q);
                if (s == null) {
                    throw invalidInput("문항 " + q + " 점수가 없습니다.");
                }
                return s;
            }).average().orElse(Double.NaN);

            subtypeAvg.put(subtypeName, avg);
            subtypeEntities.add(SubtypeScore.builder()
                    .type(subtypeName)
                    .score(avg)
                    .build());
        }

        // 상위 타입 평균
        double authoritativeAvg = mean(subtypeAvg, TYPE_TO_SUBTYPES.get(AUTHORITATIVE));
        double authoritarianAvg = mean(subtypeAvg, TYPE_TO_SUBTYPES.get(AUTHORITARIAN));
        double permissiveAvg = mean(subtypeAvg, TYPE_TO_SUBTYPES.get(PERMISSIVE));

        Map<String, Double> typeScores = new LinkedHashMap<>();
        typeScores.put(AUTHORITATIVE, authoritativeAvg);
        typeScores.put(AUTHORITARIAN, authoritarianAvg);
        typeScores.put(PERMISSIVE, permissiveAvg);

        // 최대 타입
        String userType = typeScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> invalidInput("유형을 결정할 수 없습니다."));

        // 결과 저장
        PropensityTestResult result = PropensityTestResult.builder()
                .userId(user.getUserId())
                .userType(userType)
                .listScores(toList(scoreMap))   // Map<Integer,Integer> -> List<TestListScore>
                .testScores(typeScores)
                .subtype(subtypeEntities)
                .build();

        propensityTestResultRepository.save(result);

        UserProfile userProfile = UserProfile.builder()
                .userImage(user.getUserImage())
                .userId(saved.getUserId())
                .level(1)
                .parentNickname(saved.getParentNickname())
                .points(0)
                .streakDay(0)
                .phrase("자식을 불행하게하는 가장 확실한 방법은 언제나 무엇이든지 손에 넣을 수 있게 해주는 일이다.")
                .monthFrequency(0)
                .voicereportFrequency(0)
                .chatBotFrequency(0)
                .workBookFrequency(0)
                .message("첫 방문을 환영합니다! 오늘의 첫 워크북을 시작해보세요 :)")
                .reward(new ArrayList<>())
                .usedVoiceReportOnce(0)
                .finishedWorkbookOnce(0)
                .usedChatbotOnce(0)
                .lastLoginDate(null)
                .build();
        String nowStr = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Reward firstReward = Reward.builder()
                .rewardId(1)
                .earnedAt(nowStr)
                .build();
        userProfile.getReward().add(firstReward);

        userProfileRepository.save(userProfile);

        return new UserIdDto(saved.getUserId());
    }

    // 회원 정보 확인
    @Transactional(readOnly = true)
    public UserInformationCheckDto getUserInformationById(int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> userNotFound());

        UserInformationCheckDto dto = new UserInformationCheckDto();
        dto.setId(user.getUserId());
        dto.setEmail(user.getEmail());
        dto.setParentName(user.getParentName());
        dto.setParentNickname(user.getParentNickname());
        dto.setKidsId(user.getKidsId());
        dto.setKidsNickname(user.getKidsNickname());
        dto.setKidsAge(user.getKidsAge());
        dto.setKidsTendency(user.getKidsTendency());
        dto.setKidsNote(user.getKidsNote());
        dto.setGoal(user.getGoal());
        dto.setWorry(user.getWorry());
        dto.setPersonalInformationAgree(user.getPersonalInformationAgree());
        return dto;
    }

    // 회원 정보 수정
    @Transactional
    public void modifyInformation(int userId, UserInformationModifyDto req, MultipartFile user_image) {
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> userNotFound());

        // 비밀번호 변경(선택)
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            String hashed = passwordEncoder.encode(req.getPassword());
            user.setPassword(hashed);
        }

        // user_image update
        if (user_image != null && !user_image.isEmpty()) {
            String imageUrl = fileStorageService.save(user_image);
            user.setUserImage(imageUrl);
        }

        // 기본 정보 업데이트
        user.setParentName(req.getParentName());
        user.setParentNickname(req.getParentNickname());
        user.setKidsNickname(req.getKidsNickname());
        user.setKidsAge(req.getKidsAge());
        user.setKidsTendency(req.getKidsTendency());
        user.setKidsNote(req.getKidsNote());
        user.setGoal(req.getGoal());
        user.setWorry(req.getWorry());
        user.setPersonalInformationAgree(req.getPersonalInformationAgree());

        // 유형 검사 결과 update(선택)
        if (req.getPropensityTest() == null || req.getPropensityTest().isEmpty()) {
            return;
        }

        // 점수 검증 + 수집 (int 유지)
        Map<Integer, Integer> incomingScores = new LinkedHashMap<>();
        for (UserInformationModifyDto.PropensityTestDto dto : req.getPropensityTest()) {
            int score = dto.getPropensityTestScore();
            if (score < 1 || score > 6) {
                throw scoreOutOfRange(dto.getPropensityTestId());
            }
            incomingScores.put(dto.getPropensityTestId(), score);
        }

        // 최근 결과
        PropensityTestResult result = propensityTestResultRepository
                .findTopByUserIdOrderByResultIdDesc(user.getUserId())
                .orElseThrow(() -> resultNotFound());

        // 기존 + 신규 merge
        Map<Integer, Integer> mergedScoreMap = new LinkedHashMap<>();
        if (result.getListScores() != null) {
            mergedScoreMap.putAll(toMap(result.getListScores())); // List -> Map
        }
        incomingScores.forEach(mergedScoreMap::put);

        // 서브타입 평균 재계산
        Map<String, Double> subtypeAvg = new LinkedHashMap<>();
        List<SubtypeScore> subtypeEntities = new ArrayList<>();

        for (var entry : SUBTYPE_ITEMS.entrySet()) {
            String subtypeName = entry.getKey();
            List<Integer> items = entry.getValue();

            double avg = items.stream()
                    .mapToDouble(q -> {
                        Integer s = mergedScoreMap.get(q);
                        if (s == null) {
                            throw invalidInput("문항 " + q + " 점수가 없습니다.");
                        }
                        return s;
                    })
                    .average()
                    .orElse(Double.NaN);

            subtypeAvg.put(subtypeName, avg);
            subtypeEntities.add(SubtypeScore.builder()
                    .type(subtypeName)
                    .score(avg)
                    .build());
        }

        // 상위 타입 평균
        double authoritativeAvg = mean(subtypeAvg, TYPE_TO_SUBTYPES.get(AUTHORITATIVE));
        double authoritarianAvg = mean(subtypeAvg, TYPE_TO_SUBTYPES.get(AUTHORITARIAN));
        double permissiveAvg    = mean(subtypeAvg, TYPE_TO_SUBTYPES.get(PERMISSIVE));

        Map<String, Double> typeScores = new LinkedHashMap<>();
        typeScores.put(AUTHORITATIVE, authoritativeAvg);
        typeScores.put(AUTHORITARIAN, authoritarianAvg);
        typeScores.put(PERMISSIVE, permissiveAvg);

        // 최대 타입
        String userType = typeScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> invalidInput("유형을 결정할 수 없습니다."));

        // 결과 반영 후 저장
        result.setListScores(toList(mergedScoreMap));
        result.setUserType(userType);
        result.setTestScores(typeScores);
        result.setSubtype(subtypeEntities);

        propensityTestResultRepository.save(result);
    }

    // 유형 검사 결과 불러오기
    @Transactional(readOnly = true)
    public PropensityTestResultDto getTestResultByUserId(int userId) {
        PropensityTestResult result = propensityTestResultRepository
                .findTopByUserIdOrderByResultIdDesc(userId)
                .orElseThrow(() -> resultNotFound());

        // listScores → [{ testId: score }, ...]  (DTO는 Integer 요구)
        List<Map<Integer, Integer>> listScoresDto = new ArrayList<>();
        if (result.getListScores() != null && !result.getListScores().isEmpty()) {
            for (TestListScore t : result.getListScores()) {
                listScoresDto.add(Map.of(t.getPropensityTestId(), t.getPropensityTestScore()));
            }
        }

        // testScores → [{ "authoritative": x, "authoritarian": y, "permissive": z }]
        Map<String, Double> testScoreMap = new LinkedHashMap<>();
        if (result.getTestScores() != null) {
            testScoreMap.put("authoritative", result.getTestScores().getOrDefault("authoritative", Double.NaN));
            testScoreMap.put("authoritarian", result.getTestScores().getOrDefault("authoritarian", Double.NaN));
            testScoreMap.put("permissive", result.getTestScores().getOrDefault("permissive", Double.NaN));
        }
        List<Map<String, Double>> testScoresDto = List.of(testScoreMap);

        // subtype → [{ "type": "...", "score": ... }, ...]
        List<PropensityTestResultDto.SubtypeScoreDto> subtypeDtos = new ArrayList<>();
        if (result.getSubtype() != null) {
            for (SubtypeScore s : result.getSubtype()) {
                subtypeDtos.add(PropensityTestResultDto.SubtypeScoreDto.builder()
                        .type(s.getType())
                        .score(s.getScore())
                        .build());
            }
        }

        // DTO 조립
        return PropensityTestResultDto.builder()
                .listScores(listScoresDto)
                .userType(result.getUserType())
                .testScores(testScoresDto)
                .subtype(subtypeDtos)
                .build();
    }

    // 로그인
    @Transactional(readOnly = true)
    public SessionDto login(AuthRequestDto req,
                      HttpServletRequest request,
                      HttpServletResponse response)
    {
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> loginBadCredentials());
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw loginBadCredentials();
        }

        // 1) 인증 토큰 생성
        var auth = new UsernamePasswordAuthenticationToken(
                u.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // 2) SecurityContext에 넣고, 세션에 저장
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        // 3) 세션에 userId 저장 + 세션 아이디 가져오기
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", u.getUserId());
        String sessionId = session.getId();          // ★ 여기!

        return SessionDto.builder()
                .sessionId(sessionId)
                .build();
    }

    // List<TestListScore> -> Map<Integer, Integer>
    private static Map<Integer, Integer> toMap(List<TestListScore> list) {
        Map<Integer, Integer> m = new HashMap<>();
        if (list != null) {
            for (TestListScore t : list) {
                // 동일 문항 중복 시 마지막 값으로 덮어씀
                m.put(t.getPropensityTestId(), t.getPropensityTestScore());
            }
        }
        return m;
    }

    // Map<Integer, Integer> -> List<TestListScore>
    private static List<TestListScore> toList(Map<Integer, Integer> map) {
        if (map == null) return new ArrayList<>();
        List<TestListScore> out = new ArrayList<>(map.size());
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            out.add(TestListScore.builder()
                    .propensityTestId(e.getKey())
                    .propensityTestScore(e.getValue())
                    .build());
        }
        return out;
    }
}
