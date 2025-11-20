package capstone.voicereport.repository;

import capstone.voicereport.dto.VoiceReportListResponse;
import capstone.voicereport.entity.VoiceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoiceReportRepository extends JpaRepository<VoiceReport, Long> {

    // 목록 (지금 있는 쿼리 그대로 OK)
    @Query(
            value = """
        select new capstone.voicereport.dto.VoiceReportListResponse(
            v.reportId, v.subTitle, v.day
        )
        from VoiceReport v
        where v.userId = :userId
        order by v.createdAt desc
        """,
            countQuery = """
        select count(v)
        from VoiceReport v
        where v.userId = :userId
        """
    )
    Page<VoiceReportListResponse> findByUserIdOrderByCreatedAtDesc(
            @Param("userId") int userId, Pageable pageable
    );

    Optional<VoiceReport> findByReportIdAndUserId(int reportId, int userId);
    Optional<VoiceReport> findTop1ByUserIdOrderByReportIdDesc(int userId);
    Optional<VoiceReport> findTop1ByUserIdOrderByCreatedAtDesc(int userId);
}
