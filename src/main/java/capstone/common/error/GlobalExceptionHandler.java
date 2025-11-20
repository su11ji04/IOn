package capstone.common.error;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import capstone.voicereport.error.VoiceReportException;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Data
    @AllArgsConstructor
    public static class ErrorBody {
        private String code;
        private String message;
        private String path;
    }

    // 413: 진짜 '바이트 용량 초과'만 여기로
    @ExceptionHandler({ MaxUploadSizeExceededException.class, DataBufferLimitException.class })
    public ResponseEntity<ErrorBody> handlePayloadTooLarge(Exception e, HttpServletRequest req) {
        ErrorCode code = ErrorCode.PAYLOAD_TOO_LARGE;
        log.warn("[413] payload too large: {} path={}", e.getMessage(), req.getRequestURI());
        return ResponseEntity.status(code.getStatus())
                .body(new ErrorBody(code.name(), code.getDefaultMessage(), req.getRequestURI()));
    }

    // 도메인 예외 (보이스리포트)
    @ExceptionHandler(VoiceReportException.class)
    public ResponseEntity<ErrorBody> handleVoiceReport(VoiceReportException e, HttpServletRequest req) {
        ErrorCode code = e.getCode();  // <-- 이제 게터로 접근
        String message = (e.getMessage() != null && !e.getMessage().isBlank())
                ? e.getMessage() : code.getDefaultMessage();
        log.warn("[VR] code={} status={} msg={} path={}",
                code.name(), code.getStatus().value(), message, req.getRequestURI());
        return ResponseEntity.status(code.getStatus())
                .body(new ErrorBody(code.name(), message, req.getRequestURI()));
    }

    // 그 외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleAny(Exception e, HttpServletRequest req) {
        log.error("[500] ex={} msg={} path={}", e.getClass().getName(), e.getMessage(), req.getRequestURI(), e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody(code.name(), code.getDefaultMessage(), req.getRequestURI()));
    }
}
