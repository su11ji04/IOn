package capstone.workbook.repository;

import capstone.workbook.entity.SimulationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimulationSessionRepository extends JpaRepository<SimulationSession, Long> {
    Optional<SimulationSession> findBySessionId(String sessionId);
}
