package capstone.voicereport.error;

import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VoiceReportErrorCode {

    VR_NOT_FOUND(HttpStatus.NOT_FOUND, "VR-001", "보이스 리포트를 찾을 수 없습니다."),
    VR_UPLOAD_ERROR(HttpStatus.BAD_REQUEST, "VR-002", "음성 또는 영상 업로드 중 오류가 발생했습니다."),
    VR_ANALYSIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "VR-003", "음성 분석 서버 처리 중 오류가 발생했습니다."),
    VR_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "VR-004", "분석 서버 응답이 지연되었습니다."),
    VR_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "VR-005", "요청한 사용자에게 접근 권한이 없습니다."),
    VR_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "VR-006", "로그인이 필요합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
