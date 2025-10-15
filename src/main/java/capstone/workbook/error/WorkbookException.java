package capstone.workbook.error;

import capstone.common.error.AppException;
import capstone.common.error.ErrorCode;

public class WorkbookException extends AppException {
    public WorkbookException(ErrorCode code) { super(code); }
    public WorkbookException(ErrorCode code, String message) { super(code, message); }
    public WorkbookException(ErrorCode code, String message, String details) { super(code, message, details); }

    public static WorkbookException notFound(Long id) {
        return new WorkbookException(ErrorCode.WB_NOT_FOUND, "워크북을 찾을 수 없습니다.", "id=" + id);
    }

    public static WorkbookException forbidden(String userId, String ownerId) {
        return new WorkbookException(
                ErrorCode.WB_FORBIDDEN,
                "워크북에 접근 권한이 없습니다.",
                "userId=" + userId + ", ownerId=" + ownerId
        );
    }

    // 필요 시 오버로드 (임의 상세 전달)
    public static WorkbookException forbiddenDetails(String details) {
        return new WorkbookException(ErrorCode.WB_FORBIDDEN, "워크북에 접근 권한이 없습니다.", details);
    }

    public static WorkbookException payloadEmpty() {
        return new WorkbookException(ErrorCode.WB_PAYLOAD_EMPTY, "요청 본문이 비어 있습니다.");
    }

    public static WorkbookException schemaInvalid(String details) {
        return new WorkbookException(ErrorCode.WB_SCHEMA_INVALID, "워크북 스키마 검증에 실패했습니다.", details);
    }

    public static WorkbookException generationFailed(String details) {
        return new WorkbookException(ErrorCode.WB_GENERATION_FAILED, "워크북 생성 엔진 처리에 실패했습니다.", details);
    }

    public static WorkbookException generationTimeout() {
        return new WorkbookException(ErrorCode.WB_GENERATION_TIMEOUT, "워크북 생성 엔진 응답 시간 초과입니다.");
    }

    public static WorkbookException conflictState(String details) {
        return new WorkbookException(ErrorCode.WB_CONFLICT_STATE, "현재 상태에서는 요청을 처리할 수 없습니다.", details);
    }

    public static WorkbookException badRequest(String message, String details) {
        return new WorkbookException(ErrorCode.WB_SCHEMA_INVALID, message, details);
    }
}
