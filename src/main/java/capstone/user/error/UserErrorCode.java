package capstone.user.error;

import org.springframework.http.HttpStatus;

public enum UserErrorCode {

    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "USR-001", "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USR-002", "존재하지 않는 사용자입니다."),
    LOGIN_BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USR-003", "비밀번호가 올바르지 않습니다."),
    PROPENSITY_MISSING(HttpStatus.BAD_REQUEST, "USR-004", "양육 성향 검사 결과가 누락되었습니다."),
    SCORE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "USR-005", "점수는 1~6 범위여야 합니다."),
    RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "USR-006", "유형 검사 결과가 존재하지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "USR-007", "입력값이 올바르지 않습니다."),
    JSON_PARSE_ERROR(HttpStatus.BAD_REQUEST, "USR-008", "요청 본문을 해석할 수 없습니다."),
    CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "USR-009", "필수 동의가 누락되었습니다."),
    PROPENSITY_ITEM_MISSING(HttpStatus.BAD_REQUEST, "USR-015", "일부 문항의 점수가 없습니다."),
    PROPENSITY_ITEM_DUPLICATED(HttpStatus.BAD_REQUEST, "USR-016", "중복된 문항이 포함되어 있습니다."),
    PROPENSITY_ITEM_INVALID(HttpStatus.BAD_REQUEST, "USR-017", "존재하지 않는 문항 ID가 포함되어 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    UserErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}