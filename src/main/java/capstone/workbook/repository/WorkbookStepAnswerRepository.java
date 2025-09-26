package capstone.workbook.repository;

import capstone.workbook.entity.WorkbookStepAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkbookStepAnswerRepository extends JpaRepository<WorkbookStepAnswer, Long> {
    List<WorkbookStepAnswer> findByWorkbookIdOrderByStepIndexAsc(Long workbookId);

    // ✅ 추가: 특정 스텝의 답변들을 한 번에 조회
    List<WorkbookStepAnswer> findByWorkbookIdAndStepIndex(Long workbookId, Integer stepIndex);
}