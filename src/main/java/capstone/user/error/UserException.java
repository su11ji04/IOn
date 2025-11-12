package capstone.user.error;

public class UserException extends RuntimeException {
    private final UserErrorCode code;

    public UserException(UserErrorCode code) {
        super(code.getDefaultMessage());
        this.code = code;
    }
    public UserException(UserErrorCode code, String message) {
        super((message == null || message.isBlank()) ? code.getDefaultMessage() : message);
        this.code = code;
    }
    public UserErrorCode getCode() { return code; }

    public static UserException emailDuplicate() { return new UserException(UserErrorCode.EMAIL_DUPLICATE); }
    public static UserException userNotFound() { return new UserException(UserErrorCode.USER_NOT_FOUND); }
    public static UserException loginBadCredentials() { return new UserException(UserErrorCode.LOGIN_BAD_CREDENTIALS); }
    public static UserException propensityMissing() { return new UserException(UserErrorCode.PROPENSITY_MISSING); }
    public static UserException scoreOutOfRange(int qid) {
        return new UserException(UserErrorCode.SCORE_OUT_OF_RANGE, "점수는 1~6이어야 합니다. q=" + qid);
    }
    public static UserException resultNotFound() { return new UserException(UserErrorCode.RESULT_NOT_FOUND); }
    public static UserException invalidInput(String msg) { return new UserException(UserErrorCode.INVALID_INPUT, msg); }

    public static UserException consentRequired() { return new UserException(UserErrorCode.CONSENT_REQUIRED); }
    public static UserException propensityItemMissing(String ids) {
        return new UserException(UserErrorCode.PROPENSITY_ITEM_MISSING, "점수가 누락된 문항: " + ids);
    }
    public static UserException propensityItemDuplicated(String ids) {
        return new UserException(UserErrorCode.PROPENSITY_ITEM_DUPLICATED, "중복 문항: " + ids);
    }
    public static UserException propensityItemInvalid(String ids) {
        return new UserException(UserErrorCode.PROPENSITY_ITEM_INVALID, "유효하지 않은 문항: " + ids);
    }
}