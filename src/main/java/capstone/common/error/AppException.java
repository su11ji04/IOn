package capstone.common.error;

public class AppException extends RuntimeException {
    public final ErrorCode code;
    public final String details; // 로그용

    public AppException(ErrorCode code) {
        super(code.defaultMessage);
        this.code = code;
        this.details = null;
    }
    public AppException(ErrorCode code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }
    public AppException(ErrorCode code, String message, String details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}
