package capstone.voicereport.service;

import capstone.support.userprofile.UserProfileLoader;
import capstone.user.entity.User;
import capstone.user.repository.UserRepository;
import capstone.voicereport.dto.AnalysisReportDto;
import capstone.voicereport.dto.MySpeechStyleResponse;
import capstone.voicereport.entity.*;
import capstone.voicereport.repository.VoiceReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceReportService {

    private final VoiceReportRepository voiceReportRepository;
    private final UserRepository userRepository;
    private final PythonAnalysisClient pythonAnalysisClient;
    private final UserProfileLoader userProfileLoader;

    private Path getUploadDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "voice");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    // 보이스리포트 생성
    @Transactional
    public VoiceReportResponse createVoiceReport(MultipartFile audio) throws IOException {
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("AUDIO FILE IS EMPTY");
        }

        // 임시 코드
        final String userId = "u001";
        log.info("[VOICEREPORT SERVICE] fixed userId={}", userId);

//        String userId = (String) session.getAttribute("userId");
//        if (userId == null || userId.isBlank()) {
//            throw new IllegalStateException("[VOICEREPORT SERVICE STEP1] USER ID is BLANK");
//        }
//        log.info("[VOICEREPORT SERVICE] session userId={}", userId);


        // 1) 오디오 파일 저장
        String originalName = StringUtils.cleanPath(
                audio.getOriginalFilename() == null ? "audio.wav" : audio.getOriginalFilename()
        );
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : ".wav";
        Path target = getUploadDir().resolve(UUID.randomUUID() + ext);
        Files.copy(audio.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 2) USER INFORMATION SETTING
        var userProfileMap = userProfileLoader.find(userId)
                .map(userProfileLoader::toPythonMap)
                .orElseGet(Map::of);
        log.info("[VOICEREPORT SERVICE] profile keys={})",
                userProfileMap.keySet());
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//
//        // Python 서버에 보낼 Map 형태로 변환
//        Map<String, Object> userProfileMap = Map.of(
//                "child_age", user.getChildAge(),
//                "parenting_goal", user.getParentingGoal(),
//                "preferred_tone", user.getPreferredTone(),
//                "worry", user.getWorry()
//                // 필요한 항목 더 추가
//        );
//
//        log.info("[VOICEREPORT SERVICE] profile keys={}", userProfileMap.keySet());

        // 3) Python API 호출
        AnalysisReportDto ar = null;
        try {
            ar = pythonAnalysisClient.analyze(
                    audio.getBytes(),
                    originalName,
                    userId,
                    userProfileMap
            );
            if (ar != null) {
                log.info("[VOICEREPORT SERVICE] Python 응답 수신: subTitle={}", ar.getSubTitle());
            } else {
                log.warn("[VOICEREPORT SERVICE] Python returned null");
            }
        } catch (Exception e) {
            log.error("[VOICEREPORT SERVICE] analyze call failed: {}", e.toString(), e);
        }

        // 4) 엔티티 생성 및 저장
        VoiceReport report = new VoiceReport();

        report.setAudioOriginalName(originalName);
        report.setAudioSize(audio.getSize());
        report.setAudioPath(target.toString());

        if (ar != null) {
            report.setSubTitle(nullToFallback(ar.getSubTitle(), "자동 생성 리포트"));
            if (ar.getDay() != null && !ar.getDay().isBlank()) {
                report.setDay(LocalDate.parse(ar.getDay()));
            }

            report.setConversationSummary(ar.getConversationSummary());
            report.setOverallFeedback(ar.getOverallFeedback());

            if (ar.getExpression() != null) {
                report.setExpression(Expression.builder()
                        .parentExpression(ar.getExpression().getParentExpression())
                        .kidExpression(ar.getExpression().getKidExpression())
                        .parentConditions(ar.getExpression().getParentConditions())
                        .kidConditions(ar.getExpression().getKidConditions())
                        .expressionFeedback(ar.getExpression().getExpressionFeedback())
                        .build());
            }
            if (ar.getChangeProposal() != null) {
                report.setChangeProposals(ar.getChangeProposal().stream()
                        .map(cp -> ChangeProposal.builder()
                                .existingExpression(cp.getExistingExpression())
                                .proposalExpression(cp.getProposalExpression())
                                .build())
                        .toList());
            }

            if (ar.getEmotion() != null) {
                report.setEmotionFeedback(ar.getEmotion().getEmotionFeedback());
                if (ar.getEmotion().getTimeline() != null) {
                    report.setEmotionTimeline(ar.getEmotion().getTimeline().stream()
                            .map(t -> EmotionPoint.builder()
                                    .time(t.getTime())
                                    .momentEmotion(t.getMomentEmotion())
                                    .build())
                            .toList());
                }
            }
            report.setKidAttitude(ar.getKidAttitude());

            if (ar.getFrequency() != null) {
                report.setFrequency(Frequency.builder()
                        .parentFrequency(ar.getFrequency().getParentFrequency())
                        .kidFrequency(ar.getFrequency().getKidFrequency())
                        .frequencyFeedback(ar.getFrequency().getFrequencyFeedback())
                        .build());
            }
            report.setStrength(ar.getStrength());

        } else {
            report.setSubTitle("보이스리포트_ERROR");
            report.setConversationSummary("분석 서버 오류로 요약을 생성하지 못했습니다.");
        }

        VoiceReport saved = voiceReportRepository.save(report);
        return responseToVoiceReportInquiry(saved);
    }

    // 보이스리포트 조회 (BY VOICEREPORT ID)
    @Transactional(readOnly = true)
    public VoiceReportResponse get(Long id) {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("보이스리포트 조회에 실패했습니다."));
        return responseToVoiceReportInquiry(r);
    }

    // 보이스리포트 목록 조회 (BY USER ID)
    @Transactional(readOnly = true)
    public Page<VoiceReportResponse> list(Long userId, Pageable pageable) {
        Page<VoiceReport> page = (userId == null)
                ? voiceReportRepository.findAll(pageable)
                : voiceReportRepository.findByUser_Id(userId, pageable);
        return page.map(this::responseToVoiceReportInquiry);
    }

    // 보이스리포트 삭제
    @Transactional
    public void delete(Long id) throws IOException {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("보이스리포트를 찾을 수 없습니다."));
        if (r.getAudioPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(r.getAudioPath()));
            } catch (Exception ignored) {}
        }
        voiceReportRepository.delete(r);
    }

    // 오디오 파일 경로 찾기
    @Transactional(readOnly = true)
    public Path getAudioPath(Long id) {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
        if (r.getAudioPath() == null) {
            throw new IllegalArgumentException("오디오 파일 경로가 없습니다.");
        }
        return Paths.get(r.getAudioPath());
    }

    // 보이스리포트 RETURN
    private VoiceReportResponse responseToVoiceReportInquiry(VoiceReport r) {
        List<EmotionPoint> timeline = r.getEmotionTimeline() != null ? r.getEmotionTimeline() : List.of();
        List<ChangeProposal> proposals = r.getChangeProposals() != null ? r.getChangeProposals() : List.of();

        return VoiceReportResponse.builder()
                .id(r.getId())
                .subTitle(r.getSubTitle())
                .day(String.valueOf(r.getDay()))

                .conversationSummary(r.getConversationSummary())
                .overallFeedback(r.getOverallFeedback())

                .expression(VoiceReportResponse.Expression.builder()
                        .parentExpression(r.getExpression() != null ? r.getExpression().getParentExpression() : null)
                        .kidExpression(r.getExpression() != null ? r.getExpression().getKidExpression() : null)
                        .parentConditions(r.getExpression() != null ? r.getExpression().getParentConditions() : null)
                        .kidConditions(r.getExpression() != null ? r.getExpression().getKidConditions() : null)
                        .expressionFeedback(r.getExpression() != null ? r.getExpression().getExpressionFeedback() : null)
                        .build())
                .changeProposal(proposals.stream()
                        .map(cp -> VoiceReportResponse.ChangeProposal.builder()
                                .existingExpression(cp.getExistingExpression())
                                .proposalExpression(cp.getProposalExpression())
                                .build())
                        .collect(Collectors.toList()))


                .emotion(VoiceReportResponse.Emotion.builder()
                        .timeline(timeline.stream()
                                .map(p -> VoiceReportResponse.Emotion.Timeline.builder()
                                        .time(p.getTime())
                                        .momentEmotion(p.getMomentEmotion())
                                        .build())
                                .collect(Collectors.toList()))
                        .emotionFeedback(r.getEmotionFeedback())
                        .build())
                .kidAttitude(r.getKidAttitude())

                .frequency(VoiceReportResponse.Frequency.builder()
                        .parentFrequency(r.getFrequency() != null ? r.getFrequency().getParentFrequency() : null)
                        .kidFrequency(r.getFrequency() != null ? r.getFrequency().getKidFrequency() : null)
                        .frequencyFeedback(r.getFrequency() != null ? r.getFrequency().getFrequencyFeedback() : null)
                        .build())

                .strength(r.getStrength())
                .build();

    }

    @Transactional(readOnly = true)
    public MySpeechStyleResponse buildMyStyle(Long userId, int limit) {
        // 최근 N개 리포트 조회
        Page<VoiceReport> page = voiceReportRepository.findByUser_Id(
                userId,
                PageRequest.of(0, limit, Sort.by("createdAt").descending())
        );
        List<VoiceReport> reports = page.getContent();
        if (reports.isEmpty()) {
            return MySpeechStyleResponse.builder()
                    .userId(userId)
                    .reportCount(0)
                    .overallFeedbacks(List.of())
                    .parentExpressions(List.of())
                    .topKeywords(Map.of())
                    .build();
        }

        // 최근 리포트들의 overallFeedback 모음
        List<String> overallFeedbacks = reports.stream()
                .map(VoiceReport::getOverallFeedback)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        // 부모 표현 모음
        List<String> parentExprs = reports.stream()
                .map(VoiceReport::getExpression)
                .filter(Objects::nonNull)
                .map(Expression::getParentExpression)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        // 간단 키워드 카운트 (한글/영문 단어만 추출)
        Pattern token = Pattern.compile("[\\p{IsAlphabetic}가-힣]{2,}");
        Map<String,Integer> freq = new HashMap<>();
        reports.forEach(r -> {
            String text = String.join(" ",
                    Optional.ofNullable(r.getOverallFeedback()).orElse(""),
                    Optional.ofNullable(r.getConversationSummary()).orElse(""),
                    Optional.ofNullable(r.getStrength()).orElse("")
            );
            var m = token.matcher(text);
            while (m.find()) {
                String w = m.group().toLowerCase();
                freq.merge(w, 1, Integer::sum);
            }
        });

        // 상위 10개 키워드 추출
        Map<String,Integer> topKeywords = freq.entrySet().stream()
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (x,y) -> x, LinkedHashMap::new
                ));

        return MySpeechStyleResponse.builder()
                .userId(userId)
                .reportCount(reports.size())
                .overallFeedbacks(overallFeedbacks)
                .parentExpressions(parentExprs)
                .topKeywords(topKeywords)
                .build();
    }

    // 기본값 주입
    private static String nullToFallback(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }

    // 개발용
    @Transactional
    protected User UserFromCsvProfile(String userIdFromCsv, Map<String, Object> profile) {
        String email = Optional.ofNullable(profile.get("email"))
                .map(Object::toString)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> userIdFromCsv + "@dev.local");

        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setParentNickname(
                    Optional.ofNullable(profile.get("parent_nickname"))
                            .map(Object::toString)
                            .orElse(userIdFromCsv)
            );
            u.setGoal(Optional.ofNullable(profile.get("parenting_goal")).map(Object::toString).orElse("dev"));
            u.setWorry(Optional.ofNullable(profile.get("worry")).map(Object::toString).orElse("dev"));
            u.setPasswordHash("{noop}dev");
            u.setPersonalInformationAgree(1);
            return userRepository.save(u);
        });
    }
}
