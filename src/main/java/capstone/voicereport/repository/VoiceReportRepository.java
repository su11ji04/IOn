package capstone.voicereport.repository;

import capstone.voicereport.entity.VoiceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceReportRepository extends JpaRepository<VoiceReport, Long> {
    Page<VoiceReport> findByUser_Id(Long userId, Pageable pageable);
}
