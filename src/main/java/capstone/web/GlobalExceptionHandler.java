package capstone.web;

import capstone.common.error.*;
import capstone.voicereport.service.PythonAnalysisClient;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<Object> build(HttpServletRequest req, ErrorCode code, String message, String details, Throwable ex) {
        String errorId = UUID.randomUUID().toString();

        if (code.status.is5xxServerError()) {
            log.error("[EX] id={} {} {} -> {} | {} | {}", errorId, req.getMethod(), req.getRequestURI(), code.name(), message, details, ex);
        } else {
            // 4xx라도 디버깅 필요 시 trace 남길 수 있게 상황에 따라 ex 추가
            log.warn("[EX] id={} {} {} -> {} | {} | {}", errorId, req.getMethod(), req.getRequestURI(), code.name(), message, details, ex);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("path", req.getRequestURI());
        body.put("errorId", errorId);

        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", code.name());
        err.put("message", (message != null ? message : code.defaultMessage));
        if (details != null && !details.isBlank()) err.put("details", details);
        body.put("error", err);

        return ResponseEntity.status(code.status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Object> handleApp(AppException ex, HttpServletRequest req) {
        return build(req, ex.code, ex.getMessage(), ex.details, ex);
    }

    // === Validation / Binding ===
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + "=" + fe.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b).orElse(null);
        return build(req, ErrorCode.VALIDATION_ERROR, "요청 필드 검증 실패", details, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String details = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + "=" + cv.getMessage())
                .reduce((a, b) -> a + ", " + b).orElse(null);
        return build(req, ErrorCode.VALIDATION_ERROR, "요청 제약조건 위반", details, ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String details = ex.getName() + "=" + String.valueOf(ex.getValue());
        return build(req, ErrorCode.BAD_REQUEST, "요청 파라미터 타입이 올바르지 않습니다.", details, ex);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(req, ErrorCode.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입", ex.getMessage(), ex);
    }

    // === Multipart / 파일 ===
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(req, ErrorCode.PAYLOAD_TOO_LARGE, "업로드 용량 제한 초과", ex.getMessage(), ex);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest req) {
        return build(req, ErrorCode.BAD_REQUEST, "필수 파트가 누락되었습니다.", ex.getRequestPartName(), ex);
    }

    // === Python 분석기 매핑 (세분화) ===
    @ExceptionHandler(capstone.voicereport.service.PythonAnalysisClient.PythonBadRequestException.class)
    public ResponseEntity<Object> handlePy4xx(capstone.voicereport.service.PythonAnalysisClient.PythonBadRequestException ex, HttpServletRequest req) {
        // 요청 스키마/파라미터 오류 → 400
        return build(req, ErrorCode.UPSTREAM_BAD_REQUEST, "분석기 요청이 잘못되었습니다.", ex.getMessage(), ex);
    }

    @ExceptionHandler(PythonAnalysisClient.PythonServerException.class)
    public ResponseEntity<Object> handlePy5xx(PythonAnalysisClient.PythonServerException ex, HttpServletRequest req) {
        return build(req, ErrorCode.UPSTREAM_ERROR, "분석 서버 오류", ex.getMessage(), ex);
    }


    // === 마지막 방어선 ===
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, HttpServletRequest req) {
        return build(req, ErrorCode.INTERNAL_ERROR, "서버 내부 오류", ex.getMessage(), ex);
    }
}
