package capstone.user.repository;

import capstone.user.entity.Kid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KidRepository extends JpaRepository<Kid, Long> {
}
