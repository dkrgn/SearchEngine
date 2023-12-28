package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {

    @Query(value = "SELECT * FROM lemmas WHERE lemmas.site_id = :id", nativeQuery = true)
    Optional<List<Lemma>> getAllBySiteId(int id);

    @Query(value = "SELECT * FROM lemmas", nativeQuery = true)
    Optional<List<Lemma>> getAll();

    @Query(value = "SELECT * FROM lemmas WHERE lemma = :lemma", nativeQuery = true)
    Optional<Lemma> getByLemma(String lemma);

    @Transactional
    @Modifying
    @Query(value = "UPDATE lemmas SET frequency = frequency + 1 WHERE lemma = :lemma", nativeQuery = true)
    void updateFrequency(String lemma);

    @Query(value = "SELECT MAX(frequency) FROM lemmas", nativeQuery = true)
    Optional<Integer> getHighestFrequency();
}
