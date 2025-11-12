package capstone.voicereport.error;

import capstone.common.error.AppException;
import capstone.common.error.ErrorCode;

public class VoiceReportException extends AppException {

    public VoiceReportException(ErrorCode code) { super(code); }

    public VoiceReportException(ErrorCode code, String message) { super(code, message); }

    // ====== factories ======
    public static VoiceReportException notFound(Long id) {
        return new VoiceReportException(ErrorCode.VR_NOT_FOUND,
                "보이스리포트를 찾을 수 없습니다. id=" + id);
    }

    public static VoiceReportException videoEmpty() {
        return new VoiceReportException(ErrorCode.VR_AUDIO_EMPTY);
    }

    public static VoiceReportException videoUnsupported(String msg) {
        return new VoiceReportException(
                ErrorCode.VR_AUDIO_UNSUPPORTED,
                (msg != null && !msg.isBlank()) ? msg : ErrorCode.VR_AUDIO_UNSUPPORTED.getDefaultMessage()
        );
    }

    public static VoiceReportException videoCorrupted() {
        return new VoiceReportException(ErrorCode.VR_AUDIO_CORRUPTED);
    }

    public static VoiceReportException audioTooLong(String details) {
        return new VoiceReportException(
                ErrorCode.VR_AUDIO_TOO_LONG,
                (details != null && !details.isBlank()) ? details : ErrorCode.VR_AUDIO_TOO_LONG.getDefaultMessage()
        );
    }

    public static VoiceReportException analysisTimeout() {
        return new VoiceReportException(ErrorCode.VR_ANALYSIS_TIMEOUT);
    }
}
