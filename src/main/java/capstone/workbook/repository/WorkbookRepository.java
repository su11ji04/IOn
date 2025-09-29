package capstone.workbook.repository;

import capstone.workbook.entity.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkbookRepository extends JpaRepository<Workbook, Long> {
    Page<Workbook> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
