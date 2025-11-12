package capstone.common.error;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final ErrorCode code;
    private final String details; // 로그/추적용 추가 정보

    public AppException(ErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
        this.details = null;
    }

    public AppException(ErrorCode code, String message) {
        super((message == null || message.isBlank()) ? code.getDefaultMessage() : message);
        this.code = code;
        this.details = null;
    }

    public AppException(ErrorCode code, String message, String details) {
        super((message == null || message.isBlank()) ? code.getDefaultMessage() : message);
        this.code = code;
        this.details = details;
    }

    public AppException(ErrorCode code, String message, String details, Throwable cause) {
        super((message == null || message.isBlank()) ? code.getDefaultMessage() : message, cause);
        this.code = code;
        this.details = details;
    }
}
