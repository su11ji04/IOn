package capstone.voicereport.error;

import capstone.common.error.AppException;
import capstone.common.error.ErrorCode;

public class VoiceReportException extends AppException {
    public VoiceReportException(ErrorCode code) { super(code); }
    public VoiceReportException(ErrorCode code, String message) { super(code, message); }
    public VoiceReportException(ErrorCode code, String message, String details) { super(code, message, details); }
    public VoiceReportException(ErrorCode code, String message, String details, Throwable cause) { super(code, message, details, cause); }

    // 편의 팩토리
    public static VoiceReportException loginRequired()        { return new VoiceReportException(ErrorCode.VR_LOGIN_REQUIRED); }
    public static VoiceReportException forbidden()            { return new VoiceReportException(ErrorCode.VR_FORBIDDEN); }
    public static VoiceReportException notFound()             { return new VoiceReportException(ErrorCode.VR_NOT_FOUND); }
    public static VoiceReportException payloadEmpty()         { return new VoiceReportException(ErrorCode.VR_PAYLOAD_EMPTY); }
    public static VoiceReportException unsupportedMedia()     { return new VoiceReportException(ErrorCode.VR_UNSUPPORTED_MEDIA); }
    public static VoiceReportException uploadError(String d)  { return new VoiceReportException(ErrorCode.VR_UPLOAD_ERROR, null, d); }
    public static VoiceReportException analysisError(String d){ return new VoiceReportException(ErrorCode.VR_ANALYSIS_ERROR, null, d); }
    public static VoiceReportException timeout(String d)      { return new VoiceReportException(ErrorCode.VR_TIMEOUT, null, d); }
}