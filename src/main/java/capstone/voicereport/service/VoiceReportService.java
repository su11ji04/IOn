package capstone.voicereport.service;

import capstone.support.userprofile.UserProfileLoader;
import capstone.user.entity.User;
import capstone.user.repository.UserRepository;
import capstone.voicereport.dto.CreateVoiceReportRequest;
import capstone.voicereport.dto.VoiceReportResponse;
import capstone.voicereport.entity.ChangeProposal;
import capstone.voicereport.entity.EmotionPoint;
import capstone.voicereport.entity.Expression;
import capstone.voicereport.entity.Frequency;
import capstone.voicereport.entity.VoiceReport;
import capstone.voicereport.repository.VoiceReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceReportService {

    private final VoiceReportRepository voiceReportRepository;
    private final UserRepository userRepository; // DB User 연동이 필요 없으면 제거 가능
    private final PythonAnalysisClient pythonAnalysisClient;
    private final UserProfileLoader userProfileLoader;

    private Path getUploadDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "voice");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    /**
     * 음성 파일 업로드 + Python 분석 호출 + DB 저장
     */
    @Transactional
    public VoiceReportResponse createWithFixedUser(MultipartFile audio) throws IOException {
        if (audio == null || audio.isEmpty())
            throw new IllegalArgumentException("오디오 파일이 비어 있습니다.");

        // [STEP1] 이번 단계에서 사용할 userId 고정
        final String userId = "u001";
        log.info("[STEP1][SVC] fixed userId={}", userId);


        // 1) 파일 저장
        String originalName = StringUtils.cleanPath(
                audio.getOriginalFilename() == null ? "audio.wav" : audio.getOriginalFilename()
        );
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".wav";
        Path target = getUploadDir().resolve(UUID.randomUUID() + ext);
        Files.copy(audio.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 2) CSV에서 사용자 정보 로드
        var userProfileMap = userProfileLoader.find(userId)
                .map(userProfileLoader::toPythonMap)
                .orElseGet(Map::of);
        log.info("[STEP1][SVC] profile keys={}, preview(child_age={}, preferred_tone={})",
                userProfileMap.keySet(),
                userProfileMap.get("child_age"),
                userProfileMap.get("preferred_tone"));


        log.info("[STEP2][SVC->PY] send analyze: userId={}, audioName={}, audioBytes={}, profileKeys={}, profileBytes~={}",
                userId,
                originalName,
                audio.getSize(),
                userProfileMap.keySet(),
                // 대략적인 문자열 길이
                userProfileMap.isEmpty() ? 0 : userProfileMap.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length
        );


        // 3) Python 분석 호출
        capstone.voicereport.dto.AnalysisReportDto ar = null;
        try {
            ar = pythonAnalysisClient.analyze(
                    audio.getBytes(),
                    originalName,
                    userId,
                    userProfileMap
            );
            if (ar != null) {
                log.info("[STEP4][SVC] Python 응답 매핑 OK: subTitle={}, lenSeconds={}, summary?={}, freq?={}, expr?={}, timelineLen={}",
                        ar.getSubTitle(),
                        ar.getLength(),
                        ar.getConversationSummary() != null,
                        ar.getFrequency() != null,
                        ar.getExpression() != null,
                        (ar.getEmotion() != null && ar.getEmotion().getTimeline() != null) ? ar.getEmotion().getTimeline().size() : 0
                );
            } else {
                log.warn("[STEP4][SVC] Python returned null (see PythonAnalysisClient logs)");
            }
        } catch (Exception e) {
            log.error("[STEP4][SVC] analyze call failed: {}", e.toString(), e);
        }

        // 4) 엔티티 저장 (기존 create(...) 로직과 동일)
        VoiceReport report = new VoiceReport();
        report.setAudioOriginalName(originalName);
        report.setAudioSize(audio.getSize());
        report.setAudioPath(target.toString());

        if (ar != null) {
            report.setSubTitle(nullToFallback(ar.getSubTitle(), "자동 생성 리포트"));
            if (ar.getDay() != null && !ar.getDay().isBlank()) report.setDay(ar.getDay());
            report.setConversationSummary(ar.getConversationSummary());
            report.setLengthSeconds(ar.getLength());
            report.setOverallFeedback(ar.getOverallFeedback());
            if (ar.getFrequency() != null) {
                report.setFrequency(Frequency.builder()
                        .parentFrequency(ar.getFrequency().getParentFrequency())
                        .kidFrequency(ar.getFrequency().getKidFrequency())
                        .frequencyFeedback(ar.getFrequency().getFrequencyFeedback())
                        .build());
            }
            if (ar.getExpression() != null) {
                report.setExpression(Expression.builder()
                        .parentExpression(ar.getExpression().getParentExpression())
                        .kidExpression(ar.getExpression().getKidExpression())
                        .parentConditions(ar.getExpression().getParentConditions())
                        .kidConditions(ar.getExpression().getKidConditions())
                        .expressionFeedback(ar.getExpression().getExpressionFeedback())
                        .build());
            }
            if (ar.getEmotion() != null) {
                report.setEmotionFeedback(ar.getEmotion().getEmotionFeedback());
                if (ar.getEmotion().getTimeline() != null) {
                    report.setEmotionTimeline(ar.getEmotion().getTimeline().stream()
                            .map(t -> EmotionPoint.builder().time(t.getTime()).momentEmotion(t.getMomentEmotion()).build())
                            .toList());
                }
            }
            report.setKidAttitude(ar.getKidAttitude());
            if (ar.getChangeProposal() != null) {
                report.setChangeProposals(ar.getChangeProposal().stream()
                        .map(cp -> ChangeProposal.builder()
                                .existingExpression(cp.getExistingExpression())
                                .proposalExpression(cp.getProposalExpression())
                                .build())
                        .toList());
            }
            report.setPattern(ar.getPattern());
            report.setStrength(ar.getStrength());
        } else {
            report.setSubTitle("분석 실패(임시)");
            report.setConversationSummary("분석 서버 오류로 요약을 생성하지 못했습니다.");
            report.setOverallFeedback("분석 서버 오류");
        }

        VoiceReport saved = voiceReportRepository.save(report);
        return toResponse(saved);
    }

    /**
     * 단일 조회
     */
    @Transactional(readOnly = true)
    public VoiceReportResponse get(Long id) {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
        return toResponse(r);
    }

    /**
     * 페이지 조회 (userId 필터 선택)
     */
    @Transactional(readOnly = true)
    public Page<VoiceReportResponse> list(Long userId, Pageable pageable) {
        Page<VoiceReport> page = (userId == null)
                ? voiceReportRepository.findAll(pageable)
                : voiceReportRepository.findByUser_Id(userId, pageable);
        return page.map(this::toResponse);
    }

    /**
     * 삭제 (파일 삭제 포함)
     */
    @Transactional
    public void delete(Long id) throws IOException {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
        if (r.getAudioPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(r.getAudioPath()));
            } catch (Exception ignored) {}
        }
        voiceReportRepository.delete(r);
    }

    /**
     * 저장된 오디오 파일 경로 반환
     */
    @Transactional(readOnly = true)
    public Path getAudioPath(Long id) {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
        if (r.getAudioPath() == null) {
            throw new IllegalArgumentException("오디오 파일 경로가 없습니다.");
        }
        return Paths.get(r.getAudioPath());
    }

    /**
     * 엔티티 → 응답 DTO 변환
     */
    private VoiceReportResponse toResponse(VoiceReport r) {
        List<EmotionPoint> timeline = r.getEmotionTimeline() != null ? r.getEmotionTimeline() : List.of();
        List<ChangeProposal> proposals = r.getChangeProposals() != null ? r.getChangeProposals() : List.of();

        return VoiceReportResponse.builder()
                .id(r.getId())
                .subTitle(r.getSubTitle())
                .day(r.getDay())
                .conversationSummary(r.getConversationSummary())
                .length(r.getLengthSeconds())
                .overallFeedback(r.getOverallFeedback())

                .frequency(VoiceReportResponse.Frequency.builder()
                        .parentFrequency(r.getFrequency() != null ? r.getFrequency().getParentFrequency() : null)
                        .kidFrequency(r.getFrequency() != null ? r.getFrequency().getKidFrequency() : null)
                        .frequencyFeedback(r.getFrequency() != null ? r.getFrequency().getFrequencyFeedback() : null)
                        .build())

                .expression(VoiceReportResponse.Expression.builder()
                        .parentExpression(r.getExpression() != null ? r.getExpression().getParentExpression() : null)
                        .kidExpression(r.getExpression() != null ? r.getExpression().getKidExpression() : null)
                        .parentConditions(r.getExpression() != null ? r.getExpression().getParentConditions() : null)
                        .kidConditions(r.getExpression() != null ? r.getExpression().getKidConditions() : null)
                        .expressionFeedback(r.getExpression() != null ? r.getExpression().getExpressionFeedback() : null)
                        .build())

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

                .changeProposal(proposals.stream()
                        .map(cp -> VoiceReportResponse.ChangeProposal.builder()
                                .existingExpression(cp.getExistingExpression())
                                .proposalExpression(cp.getProposalExpression())
                                .build())
                        .collect(Collectors.toList()))

                .pattern(r.getPattern())
                .strength(r.getStrength())
                .build();
    }

    private static String nullToFallback(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v;
    }


    @Transactional
    protected User ensureUserFromCsvProfile(String userIdFromCsv, Map<String, Object> profile) {
        // CSV에 email 컬럼이 없다면 규칙 메일로 생성
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
            u.setPasswordHash("{noop}dev");      // 개발용 더미
            u.setPersonalInformationAgree(1);    // 개발용 동의
            return userRepository.save(u);
        });
    }
}
