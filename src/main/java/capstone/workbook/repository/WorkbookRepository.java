package capstone.workbook.repository;

import capstone.workbook.dto.WorkbookListResponse;
import capstone.workbook.entity.Workbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkbookRepository extends JpaRepository<Workbook, Long> {

    @Query(
            value = """
            select new capstone.workbook.dto.WorkbookListResponse(
                w.chapterId, w.id, w.activityTitle
            )
            from Workbook w
            where w.userId = :userId
            order by w.createdAt desc
            """,
            countQuery = """
            select count(w)
            from Workbook w
            where w.userId = :userId
            """
    )
    Page<WorkbookListResponse> findByUserIdOrderByCreatedAtDesc(
            @Param("userId") String userId, Pageable pageable
    );

    @Query(
            value = """
            select new capstone.workbook.dto.WorkbookListResponse(
                w.chapterId, w.id, w.activityTitle
            )
            from Workbook w
            where w.userId = :userId and w.chapterId = :chapterId
            order by w.createdAt desc
            """,
            countQuery = """
            select count(w)
            from Workbook w
            where w.userId = :userId and w.chapterId = :chapterId
            """
    )
    Page<WorkbookListResponse> findByUserIdAndChapterIdOrderByCreatedAtDesc(
            @Param("userId") String userId,
            @Param("chapterId") int chapterId,
            Pageable pageable
    );
}
