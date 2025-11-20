package capstone.home.repository;

import capstone.home.entity.Phrase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PhraseRepository extends JpaRepository<Phrase, Integer> {

    @Query(value = "SELECT * FROM phrase ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<Phrase> findRandomOne();
}