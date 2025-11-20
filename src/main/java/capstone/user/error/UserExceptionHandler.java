package capstone.user.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestControllerAdvice(basePackages = "capstone.user.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {

    private static ResponseEntity<UserErrorResponse> build(UserErrorCode ec, String msg, String path,
                                                           List<UserErrorResponse.FieldError> fes) {
        return ResponseEntity
                .status(ec.getStatus())
                .body(UserErrorResponse.of(ec, msg, path, fes));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<UserErrorResponse> handleUser(UserException ex, HttpServletRequest req) {
        UserErrorCode ec = ex.getCode();
        return build(ec, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<UserErrorResponse> handleValid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<UserErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new UserErrorResponse.FieldError(f.getField(), f.getDefaultMessage(), f.getRejectedValue()))
                .toList();
        return build(UserErrorCode.INVALID_INPUT, "입력값이 올바르지 않습니다.", req.getRequestURI(), fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<UserErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<UserErrorResponse.FieldError> fields = ex.getConstraintViolations().stream()
                .map(v -> new UserErrorResponse.FieldError(v.getPropertyPath().toString(), v.getMessage(), v.getInvalidValue()))
                .toList();
        return build(UserErrorCode.INVALID_INPUT, "입력값이 올바르지 않습니다.", req.getRequestURI(), fields);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<UserErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        var fe = new UserErrorResponse.FieldError(ex.getParameterName(), "필수 파라미터가 누락되었습니다.", null);
        return build(UserErrorCode.INVALID_INPUT, null, req.getRequestURI(), List.of(fe));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<UserErrorResponse> handleJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(UserErrorCode.JSON_PARSE_ERROR, null, req.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<UserErrorResponse> handleIllegal(IllegalArgumentException ex, HttpServletRequest req) {
        return build(UserErrorCode.INVALID_INPUT, ex.getMessage(), req.getRequestURI(), null);
    }
}