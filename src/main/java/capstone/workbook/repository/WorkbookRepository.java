package capstone.workbook.repository;

import capstone.workbook.dto.LessonDto;
import capstone.workbook.entity.Workbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkbookRepository extends JpaRepository<Workbook, Integer> {

    @Query("""
        select new capstone.workbook.dto.LessonDto(
            w.lessonId,
            w.lessonTitle,
            w.done
        )
        from Workbook w
        where w.userId = :userId
          and w.chapterId = :chapterId
        order by w.lessonId asc
    """)
    List<LessonDto> findLessons(
            @Param("userId") int userId,
            @Param("chapterId") int chapterId
    );

    Workbook findByUserIdAndChapterIdAndLessonId(int userId, int chapterId, int lessonId);

    Workbook findTopByUserIdAndChapterIdOrderByLessonIdAsc(int userId, int chapterId);
}


