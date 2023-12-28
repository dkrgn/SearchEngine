package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {

    @Query(value = "SELECT page_id FROM indices WHERE lemma_id = :id", nativeQuery = true)
    Optional<List<Integer>> getAllPagesByLemmaId(int id);

    @Query(value = "SELECT SUM(ranking) from indices WHERE lemma_id = :id", nativeQuery = true)
    Optional<Float> getRankingsByLemmaId(int id);

    @Query(value = "SELECT COUNT(*) FROM indices WHERE lemma_id = :id",nativeQuery = true)
    Optional<Integer> getSumAllPagesByLemmaId(int id);

    @Query(value = "SELECT ranking FROM indices WHERE lemma_id = :lemmaId AND page_id = :pageId",nativeQuery = true)
    Optional<Float> getRankByPageIdAndLemmaId(int lemmaId, int pageId);
}
