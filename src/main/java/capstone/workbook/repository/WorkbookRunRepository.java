package capstone.workbook.repository;

import capstone.workbook.entity.WorkbookRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkbookRunRepository extends JpaRepository<WorkbookRun, Long> {
    Optional<WorkbookRun> findTopByWorkbookIdOrderByIdDesc(Long workbookId);
}