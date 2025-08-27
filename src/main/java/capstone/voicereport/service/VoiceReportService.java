package capstone.voicereport.service;

import capstone.user.entity.User;
import capstone.user.repository.UserRepository;
import capstone.voicereport.dto.CreateVoiceReportRequest;
import capstone.voicereport.dto.VoiceReportResponse;
import capstone.voicereport.entity.*;
import capstone.voicereport.repository.VoiceReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VoiceReportService {

    private final VoiceReportRepository voiceReportRepository;
    private final UserRepository userRepository;
    private final PythonAnalysisClient pythonAnalysisClient;

    private Path getUploadDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "voice");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    @Transactional
    public VoiceReportResponse create(MultipartFile audio,
                                      CreateVoiceReportRequest req) throws IOException {

        if (audio == null || audio.isEmpty()) throw new IllegalArgumentException("오디오 파일이 비어 있습니다.");
        if (req == null) throw new IllegalArgumentException("요청 메타데이터가 필요합니다.");

        // 1) 파일 저장
        String originalName = StringUtils.cleanPath(audio.getOriginalFilename() == null ? "audio" : audio.getOriginalFilename());
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot > -1) ext = originalName.substring(dot);
        String saveName = UUID.randomUUID() + ext;

        Path target = getUploadDir().resolve(saveName);
        Files.copy(audio.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 2) Python 분석 호출 (userId → 필요한 형식으로 매핑)
        String userIdForPython = (req.getUserId() != null) ? "u" + req.getUserId() : null;
        var ar = pythonAnalysisClient.analyze(
                audio.getBytes(), originalName, req.getSubTitle(), userIdForPython
        );

        // 3) 엔티티 구성 + 분석 결과 반영
        VoiceReport report = new VoiceReport();
        report.setAudioOriginalName(originalName);
        report.setAudioSize(audio.getSize());
        report.setAudioPath(target.toString());

        if (req.getUserId() != null) {
            User user = userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            report.setUser(user);
        }

        report.setSubTitle(ar.getSubTitle());
        report.setDay(ar.getDay());
        report.setConversationSummary(ar.getConversationSummary());
        report.setLengthSeconds(ar.getLength());
        report.setOverallFeedback(ar.getOverallFeedback());

        if (ar.getFrequency() != null) {
            report.setFrequency(capstone.voicereport.entity.Frequency.builder()
                    .parentFrequency(ar.getFrequency().getParentFrequency())
                    .kidFrequency(ar.getFrequency().getKidFrequency())
                    .frequencyFeedback(ar.getFrequency().getFrequencyFeedback())
                    .build());
        }

        if (ar.getExpression() != null) {
            report.setExpression(capstone.voicereport.entity.Expression.builder()
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
                        .map(t -> EmotionPoint.builder()
                                .time(t.getTime())
                                .momentEmotion(t.getMomentEmotion())
                                .build())
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

        VoiceReport saved = voiceReportRepository.save(report);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VoiceReportResponse get(Long id) {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public Page<VoiceReportResponse> list(Long userId, Pageable pageable) {
        Page<VoiceReport> page = (userId == null)
                ? voiceReportRepository.findAll(pageable)
                : voiceReportRepository.findByUser_Id(userId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public void delete(Long id) throws IOException {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
        // 파일 삭제 (있으면)
        if (r.getAudioPath() != null) {
            try { Files.deleteIfExists(Paths.get(r.getAudioPath())); } catch (Exception ignored) {}
        }
        voiceReportRepository.delete(r);
    }

    // 오디오 파일 경로 반환 (컨트롤러에서 스트림)
    @Transactional(readOnly = true)
    public Path getAudioPath(Long id) {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));
        if (r.getAudioPath() == null) throw new IllegalArgumentException("오디오 파일 경로가 없습니다.");
        return Paths.get(r.getAudioPath());
    }

    private VoiceReportResponse toResponse(VoiceReport r) {
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
                        .timeline(r.getEmotionTimeline().stream()
                                .map(p -> VoiceReportResponse.Emotion.Timeline.builder()
                                        .time(p.getTime())
                                        .momentEmotion(p.getMomentEmotion())
                                        .build())
                                .toList())
                        .emotionFeedback(r.getEmotionFeedback())
                        .build())
                .kidAttitude(r.getKidAttitude())
                .changeProposal(r.getChangeProposals().stream()
                        .map(cp -> VoiceReportResponse.ChangeProposal.builder()
                                .existingExpression(cp.getExistingExpression())
                                .proposalExpression(cp.getProposalExpression())
                                .build())
                        .toList())
                .pattern(r.getPattern())
                .strength(r.getStrength())
                .build();
    }
}
