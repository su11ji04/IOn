package capstone.voicereport.repository;

import capstone.voicereport.dto.VoiceReportListResponse;
import capstone.voicereport.entity.VoiceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoiceReportRepository extends JpaRepository<VoiceReport, Integer> {



    @Query(
            value = """
            select new capstone.voicereport.dto.VoiceReportListResponse(
                v.id, v.subTitle, v.day
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
            @Param("userId") String userId, Pageable pageable
    );
}

