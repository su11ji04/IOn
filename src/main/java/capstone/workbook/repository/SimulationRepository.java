package capstone.workbook.repository;

import capstone.workbook.entity.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, Integer> {
    Optional<Simulation> findByUserIdAndChapterIdAndLessonId(int userId, int chapterId, int lessonId);

}
