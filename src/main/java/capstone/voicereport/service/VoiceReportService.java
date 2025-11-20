package capstone.voicereport.service;

import capstone.home.entity.UserProfile;
import capstone.home.repository.UserProfileRepository;
import capstone.user.entity.User;
import capstone.user.repository.UserRepository;
import capstone.voicereport.dto.VoiceReportListResponse;
import capstone.voicereport.dto.VoiceReportResponse;
import capstone.voicereport.dto.VoiceReportSummaryDto;
import capstone.voicereport.entity.*;
import capstone.voicereport.error.VoiceReportException;
import capstone.voicereport.repository.VoiceReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceReportService {

    private final VoiceReportRepository voiceReportRepository;
    private final PythonAnalysisClient pythonAnalysisClient;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Value("${media.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;
    @Value("${media.convert.timeoutSec:120}")
    private long convertTimeoutSec;

    private Path getUploadDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "voice");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    // 파일 변환(영상 -> 음성)
    private void convertToWav16kMono(Path input, Path outWav) {
        List<String> cmd = List.of(
                ffmpegPath, "-y",
                "-i", input.toAbsolutePath().toString(),
                "-vn", "-ac", "1",
                "-ar", "16000",
                "-acodec", "pcm_s16le",
                "-f", "wav",
                outWav.toAbsolutePath().toString()
        );

        StringBuilder err = new StringBuilder();
        try {
            Process p = new ProcessBuilder(cmd).start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) err.append(line).append(System.lineSeparator());
            }
            boolean ok = p.waitFor(convertTimeoutSec, TimeUnit.SECONDS);
            if (!ok) { p.destroyForcibly(); throw new IllegalStateException("ffmpeg timeout (" + convertTimeoutSec + "s)"); }
            if (p.exitValue() != 0) throw new IllegalStateException("ffmpeg failed (exit=" + p.exitValue() + ")\n" + err);
            if (!Files.exists(outWav) || Files.size(outWav) == 0) throw new IllegalStateException("ffmpeg produced empty output");
            log.info("[VOICEREPORT] ffmpeg OK -> {}", outWav);
        } catch (Exception e) {
            FileUtils.deleteQuietly(outWav.toFile());
            throw new RuntimeException("Audio convert failed: " + e.getMessage() + "\n" + err, e);
        }
    }

    // 보이스리포트 응답 DTO 생성
    private VoiceReportResponse toDto(VoiceReport r) {
        List<EmotionPoint> timeline = (r.getEmotionTimeline() != null) ? r.getEmotionTimeline() : List.of();
        List<ChangeProposal> proposals = (r.getChangeProposals() != null) ? r.getChangeProposals() : List.of();

        int userId = r.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        VoiceReportResponse resp = new VoiceReportResponse();
        resp.setKidsNickname(user.getKidsNickname());
        resp.setReportId(r.getReportId());
        resp.setSubTitle(r.getSubTitle());
        resp.setDay(r.getDay());
        resp.setConversationSummary(r.getConversationSummary());
        resp.setOverallFeedback(r.getOverallFeedback());

        VoiceReportResponse.ExpressionDto ex = new VoiceReportResponse.ExpressionDto();
        if (r.getExpression() != null) {
            ex.setParentExpression(r.getExpression().getParentExpression());
            ex.setKidExpression(r.getExpression().getKidExpression());
            ex.setParentConditions(r.getExpression().getParentConditions());
            ex.setKidConditions(r.getExpression().getKidConditions());
            ex.setExpressionFeedback(r.getExpression().getExpressionFeedback());
        }
        resp.setExpression(ex);

        resp.setChangeProposal(
                proposals.stream().map(cp -> {
                    VoiceReportResponse.ChangeProposalDto d = new VoiceReportResponse.ChangeProposalDto();
                    d.setExistingExpression(cp.getExistingExpression());
                    d.setProposalExpression(cp.getProposalExpression());
                    return d;
                }).toList()
        );

        VoiceReportResponse.EmotionDto em = new VoiceReportResponse.EmotionDto();
        em.setTimeline(
                timeline.stream().map(p -> {
                    VoiceReportResponse.EmotionPointDto t = new VoiceReportResponse.EmotionPointDto();
                    t.setTime(p.getTime());
                    t.setMomentEmotion(p.getMomentEmotion());
                    return t;
                }).toList()
        );
        em.setEmotionFeedback(r.getEmotionFeedback());
        resp.setEmotion(em);

        resp.setKidAttitude(r.getKidAttitude());

        VoiceReportResponse.FrequencyDto fq = new VoiceReportResponse.FrequencyDto();
        if (r.getFrequency() != null) {
            fq.setParentFrequency(r.getFrequency().getParentFrequency());
            fq.setKidFrequency(r.getFrequency().getKidFrequency());
            fq.setFrequencyFeedback(r.getFrequency().getFrequencyFeedback());
        }
        resp.setFrequency(fq);

        resp.setStrength(r.getStrength());
        return resp;
    }

    // 보이스리포트 생성
    @Transactional
    public VoiceReportResponse createVoiceReportFromVideo(int userId, MultipartFile video) {
        if (video == null || video.isEmpty()) {
            throw VoiceReportException.uploadError("empty multipart file");
        }
        final String originalName = StringUtils.cleanPath(
                video.getOriginalFilename() == null ? "upload" : video.getOriginalFilename()
        );
        final String inExt = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        Path inTemp = null;
        Path outWav = null;

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> VoiceReportException.uploadError("user not found: " + userId));

        try {
            // 임시 파일 저장
            inTemp = Files.createTempFile("in_", inExt);
            outWav = Files.createTempFile("vr_", ".wav");
            try (var in = video.getInputStream()) {
                Files.copy(in, inTemp, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw VoiceReportException.uploadError("store temp: " + e.getMessage());
            }

            // ffmpeg 변환 (convertError 없으므로 uploadError로 통일)
            try {
                convertToWav16kMono(inTemp, outWav);
            } catch (Exception e) {
                throw VoiceReportException.uploadError("ffmpeg: " + e.getMessage());
            }

            // Python 분석
            VoiceReportResponse ar;
            try {
                byte[] wavBytes = Files.readAllBytes(outWav);
                ar = pythonAnalysisClient.analyze(wavBytes, userId);
            } catch (java.net.SocketTimeoutException te) {
                throw VoiceReportException.timeout("python analyze timeout");
            } catch (Exception e) {
                throw VoiceReportException.analysisError("python analyze: " + e.getMessage());
            }

            // 엔티티 매핑
            VoiceReport report = new VoiceReport();
            report.setUserId(userId);

            if (ar == null) {
                report.setSubTitle("보이스리포트_ERROR");
                report.setConversationSummary("분석 서버 응답이 비어 있습니다.");
            } else {
                report.setSubTitle(ar.getSubTitle());
                report.setDay(ar.getDay());
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
                    report.setChangeProposals(
                            ar.getChangeProposal().stream()
                                    .map(cp -> ChangeProposal.builder()
                                            .existingExpression(cp.getExistingExpression())
                                            .proposalExpression(cp.getProposalExpression())
                                            .build())
                                    .toList()
                    );
                }

                if (ar.getEmotion() != null) {
                    report.setEmotionFeedback(ar.getEmotion().getEmotionFeedback());
                    if (ar.getEmotion().getTimeline() != null) {
                        report.setEmotionTimeline(
                                ar.getEmotion().getTimeline().stream()
                                        .map(t -> EmotionPoint.builder()
                                                .time(t.getTime())
                                                .momentEmotion(t.getMomentEmotion())
                                                .build())
                                        .toList()
                        );
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
            }

            // 저장
            VoiceReport saved = voiceReportRepository.save(report);

            // 응답 DTO로 변환
            VoiceReportResponse resp = toDto(saved);

            // kidsNickname
            String kidsNickname = (ar != null && ar.getKidsNickname() != null && !ar.getKidsNickname().isBlank())
                    ? ar.getKidsNickname()
                    : user.getKidsNickname();
            if (resp != null) {
                resp.setKidsNickname(kidsNickname);
            }

            UserProfile p = userProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 유저 프로필이 존재하지 않습니다."));
            p.setUsedVoiceReportOnce(1);
            int vFrequency = p.getVoicereportFrequency() + 1;
            p.setVoicereportFrequency(vFrequency);
            int nowPoints = p.getPoints();
            p.setPoints(nowPoints+4);

            return toDto(saved);

        } catch (IOException ioe) {
            throw VoiceReportException.uploadError("io: " + ioe.getMessage());
        } finally {
            try { if (inTemp != null) Files.deleteIfExists(inTemp); } catch (Exception ignore) {}
            try { if (outWav != null) Files.deleteIfExists(outWav); } catch (Exception ignore) {}
        }
    }

    // 보이스리포트 단건 조회
    @Transactional(readOnly = true)
    public VoiceReportResponse getOneVoicereport(int userId, int reportId) {
        VoiceReport r = voiceReportRepository
                .findByReportIdAndUserId(reportId, userId)
                .orElseThrow(VoiceReportException::notFound);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> VoiceReportException.uploadError("user not found: " + userId));
        VoiceReportResponse dto = toDto(r);
        dto.setKidsNickname(user.getKidsNickname());
        return dto;
    }

    // 보이스리포트 목록 조회
    @Transactional(readOnly = true)
    public Page<VoiceReportListResponse> list(int userId, Pageable pageable) {
        return voiceReportRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // 보이스리포트 요약
    @Transactional(readOnly = true)
    public VoiceReportSummaryDto getSummary(int userId) {
        VoiceReport r = voiceReportRepository
                .findTop1ByUserIdOrderByReportIdDesc(userId)
                .orElseThrow(VoiceReportException::notFound);

        VoiceReportSummaryDto dto = new VoiceReportSummaryDto();

        // momentEmotion
        List<EmotionPoint> timeline = Optional.ofNullable(r.getEmotionTimeline()).orElseGet(List::of);
        if (!timeline.isEmpty()) {
            int idx = ThreadLocalRandom.current().nextInt(timeline.size());
            dto.setMomentEmotion(timeline.get(idx).getMomentEmotion());
        } else {
            dto.setMomentEmotion(null);
        }

        // Frequency
        if (r.getFrequency() != null) {
            dto.setParentFrequency(r.getFrequency().getParentFrequency());
            dto.setKidFrequency(r.getFrequency().getKidFrequency());
        } else {
            dto.setParentFrequency(null);
            dto.setKidFrequency(null);
        }

        // ChangeProposal
        List<ChangeProposal> cps = Optional.ofNullable(r.getChangeProposals()).orElseGet(List::of);
        if (r.getOverallFeedback() != null) {
            dto.setOverallFeedback(r.getOverallFeedback());
        } else {
            dto.setOverallFeedback(null);
        }

        return dto;
    }
}