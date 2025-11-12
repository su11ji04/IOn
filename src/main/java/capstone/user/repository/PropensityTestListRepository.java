package capstone.user.repository;

import capstone.user.entity.PropensityTestList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropensityTestListRepository extends JpaRepository<PropensityTestList, Integer> {}
