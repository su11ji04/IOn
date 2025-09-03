package capstone.workbook.repository;

import capstone.workbook.entity.Workbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkbookRepository extends JpaRepository<Workbook, Long> {
    List<Workbook> findTop20ByUserIdOrderByCreatedAtDesc(String userId);
}
