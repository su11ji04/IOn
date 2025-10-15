package capstone.voicereport.service;

import capstone.support.userprofile.UserProfileLoader;
import capstone.user.repository.UserRepository;
import capstone.voicereport.dto.VoiceReportListResponse;
import capstone.voicereport.dto.VoiceReportResponse;
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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceReportService {

    private final VoiceReportRepository voiceReportRepository;
    private final PythonAnalysisClient pythonAnalysisClient;

    @Value("${media.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${media.convert.timeoutSec:120}")
    private long convertTimeoutSec;

    private Path getUploadDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "voice");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    // 영상 -> 음성 변환
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
        List<EmotionPoint> timeline = r.getEmotionTimeline() != null ? r.getEmotionTimeline() : List.of();
        List<ChangeProposal> proposals = r.getChangeProposals() != null ? r.getChangeProposals() : List.of();

        VoiceReportResponse resp = new VoiceReportResponse();
        resp.setId(r.getId());
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
    public VoiceReportResponse createVoiceReportFromVideo(String userId, MultipartFile video) throws IOException {
        if (video == null || video.isEmpty()) {
            throw VoiceReportException.videoEmpty();
        }

        final String originalName = StringUtils.cleanPath(
                video.getOriginalFilename() == null ? "upload" : video.getOriginalFilename()
        );
        final String inExt = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        final Path inTemp = Files.createTempFile("in_", inExt);
        Files.copy(video.getInputStream(), inTemp, StandardCopyOption.REPLACE_EXISTING);

        final Path outWav = Files.createTempFile("vr_", ".wav");

        try {
            // 영상 -> 오디오 변환
            try {
                convertToWav16kMono(inTemp, outWav);
            } catch (RuntimeException e) {
                // ffmpeg 오류
                throw VoiceReportException.videoCorrupted();
            }

            // Python 분석 요청
            byte[] wavBytes = Files.readAllBytes(outWav);
            VoiceReportResponse ar;
            try {
                ar = pythonAnalysisClient.analyze(wavBytes, userId);
            } catch (PythonAnalysisClient.PythonBadRequestException e) {
                throw VoiceReportException.videoUnsupported("python 400: " + e.getMessage());
            } catch (PythonAnalysisClient.PythonServerException e) {
                throw VoiceReportException.analysisTimeout(); // 분석 엔진 오류 or timeout
            } catch (Exception e) {
                throw VoiceReportException.analysisTimeout();
            }

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

            VoiceReport saved = voiceReportRepository.save(report);
            return toDto(saved);

        } finally {
            try { Files.deleteIfExists(inTemp); } catch (Exception ignore) {}
            try { Files.deleteIfExists(outWav); } catch (Exception ignore) {}
        }
    }

    @Transactional(readOnly = true)
    public VoiceReportResponse get(Long id) {
        VoiceReport r = voiceReportRepository.findById(id)
                .orElseThrow(() -> VoiceReportException.notFound(id));
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public Page<VoiceReportListResponse> list(String userId, Pageable pageable) {
        return voiceReportRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
