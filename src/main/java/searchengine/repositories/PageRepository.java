package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageModel, Integer> {

    @Query(value = "SELECT * FROM pages", nativeQuery = true)
    Optional<List<PageModel>> getAll();

    @Query(value = "SELECT * FROM pages WHERE site_id = :id", nativeQuery = true)
    Optional<List<PageModel>> getAllBySiteId(int id);

    @Query(value = "SELECT * FROM pages WHERE path = :path AND site_id = :siteId", nativeQuery = true)
    Optional<PageModel> getByPathAndId(String path, int siteId);
}