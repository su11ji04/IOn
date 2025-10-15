package capstone.voicereport.error;

import capstone.common.error.AppException;
import capstone.common.error.ErrorCode;

public class VoiceReportException extends AppException {
    public VoiceReportException(ErrorCode code) { super(code); }
    public VoiceReportException(ErrorCode code, String message) { super(code, message); }
    public VoiceReportException(ErrorCode code, String message, String details) { super(code, message, details); }

    // 리포트 조회 실패
    public static VoiceReportException notFound(Long id) {
        return new VoiceReportException(ErrorCode.VR_NOT_FOUND, "리포트를 찾을 수 없습니다.", "id=" + id);
    }

    // 영상 업로드 관련
    public static VoiceReportException videoEmpty() {
        return new VoiceReportException(ErrorCode.VR_AUDIO_EMPTY, "업로드한 영상이 비어 있습니다.");
    }


    public static VoiceReportException videoUnsupported(String mime) {
        return new VoiceReportException(ErrorCode.VR_AUDIO_UNSUPPORTED, "지원하지 않는 영상 포맷입니다.", "mime=" + mime);
    }

    public static VoiceReportException videoCorrupted() {
        return new VoiceReportException(ErrorCode.VR_AUDIO_CORRUPTED, "영상 파일이 손상되었습니다.");
    }

    // 분석 관련
    public static VoiceReportException analysisTimeout() {
        return new VoiceReportException(ErrorCode.VR_ANALYSIS_TIMEOUT, "분석 엔진 응답 시간 초과입니다.");
    }
}
