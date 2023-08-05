package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaModel;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaModel, Integer> {

    @Query(value = "SELECT * FROM lemmas WHERE lemmas.site_id = :id", nativeQuery = true)
    Optional<List<LemmaModel>> getAllBySiteId(int id);

    @Query(value = "SELECT * FROM lemmas", nativeQuery = true)
    Optional<List<LemmaModel>> getAll();

    @Query(value = "SELECT * FROM lemmas WHERE lemma = :lemma", nativeQuery = true)
    Optional<Object> getByLemma(String lemma);

    @Transactional
    @Modifying
    @Query(value = "UPDATE lemmas SET frequency = frequency + 1 WHERE lemma = :lemma", nativeQuery = true)
    void updateFrequency(String lemma);
}
