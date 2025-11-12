// capstone.user.repository.PropensityTestResultRepository.java
package capstone.user.repository;

import capstone.user.entity.PropensityTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PropensityTestResultRepository extends JpaRepository<PropensityTestResult, Integer> {
    Optional<PropensityTestResult> findTopByUserIdOrderByResultIdDesc(Integer userId);
}
