package capstone.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 공통
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),                           // 400
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 필드 검증에 실패했습니다."),             // 400
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),                         // 401
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),                             // 403
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),                         // 404
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),       // 405
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."), // 415
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "업로드 용량 제한을 초과했습니다."),     // 413
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다."),              // 429

    // 업스트림/네트워크
    UPSTREAM_BAD_REQUEST(HttpStatus.BAD_REQUEST, "업스트림 요청이 잘못되었습니다."),       // 400
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "업스트림 오류가 발생했습니다."),               // 502
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스를 일시적으로 사용할 수 없습니다."), // 503
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "업스트림 응답 시간 초과입니다."),         // 504

    // 파싱/직렬화
    PARSE_ERROR(HttpStatus.BAD_REQUEST, "요청/응답 파싱에 실패했습니다."),                 // 400

    // 최종
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),   // 500

    // 보이스리포트
    VR_NOT_FOUND(HttpStatus.NOT_FOUND, "보이스 리포트를 찾을 수 없습니다."),                 // 404
    VR_AUDIO_EMPTY(HttpStatus.BAD_REQUEST, "업로드한 영상이 비어 있습니다."),            // 400
    VR_AUDIO_TOO_LONG(HttpStatus.BAD_REQUEST, "허용 길이를 초과한 영상 파일 입니다."),         // 400
    VR_AUDIO_UNSUPPORTED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 영상 포맷입니다."), // 415
    VR_AUDIO_CORRUPTED(HttpStatus.BAD_REQUEST, "영상 파일이 손상되었습니다."),          // 400
    VR_ANALYSIS_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "분석 엔진 응답 시간 초과입니다."),    // 504

    // 워크북
    WB_NOT_FOUND(org.springframework.http.HttpStatus.NOT_FOUND, "워크북을 찾을 수 없습니다."),                 // 404
    WB_FORBIDDEN(org.springframework.http.HttpStatus.FORBIDDEN, "워크북에 접근 권한이 없습니다."),           // 403
    WB_PAYLOAD_EMPTY(org.springframework.http.HttpStatus.BAD_REQUEST, "요청 본문이 비어 있습니다."),         // 400
    WB_SCHEMA_INVALID(org.springframework.http.HttpStatus.BAD_REQUEST, "워크북 스키마 검증에 실패했습니다."), // 400
    WB_GENERATION_FAILED(org.springframework.http.HttpStatus.BAD_GATEWAY, "워크북 생성 엔진 처리에 실패했습니다."), // 502
    WB_GENERATION_TIMEOUT(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT, "워크북 생성 엔진 응답 시간 초과입니다."), // 504
    WB_CONFLICT_STATE(org.springframework.http.HttpStatus.CONFLICT, "현재 상태에서는 요청을 처리할 수 없습니다."); // 409


    public final HttpStatus status;
    public final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
}
