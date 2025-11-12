package capstone.user.repository;

import capstone.user.dto.PropensityTestContentDto;
import capstone.user.entity.PropensityTestList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PropensityTestRepository extends JpaRepository<PropensityTestList, Integer> {

    @Query("select new capstone.user.dto.PropensityTestContentDto(p.propensityTestId, p.propensityTestQuestion) " +
            "from PropensityTestList p")
    List<PropensityTestContentDto> findAllAsContentDto();
}

