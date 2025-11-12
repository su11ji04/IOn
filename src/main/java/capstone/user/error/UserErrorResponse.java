package capstone.user.error;

import java.time.OffsetDateTime;
import java.util.List;

public class UserErrorResponse {
    public static class FieldError {
        public String field;
        public String message;
        public Object rejectedValue;
        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field; this.message = message; this.rejectedValue = rejectedValue;
        }
    }

    public OffsetDateTime timestamp;
    public int status;
    public String error;
    public String code;
    public String message;
    public String path;
    public List<FieldError> errors;

    public static UserErrorResponse of(UserErrorCode ec, String message, String path,
                                       List<FieldError> fieldErrors) {
        UserErrorResponse r = new UserErrorResponse();
        r.timestamp = OffsetDateTime.now();
        r.status = ec.getStatus().value();
        r.error = ec.getStatus().getReasonPhrase();
        r.code = ec.getCode();
        r.message = (message == null || message.isBlank()) ? ec.getDefaultMessage() : message;
        r.path = path;
        r.errors = fieldErrors;
        return r;
    }
}