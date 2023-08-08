package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Site;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Query(value = "SELECT * FROM sites AS s WHERE s.url = :url", nativeQuery = true)
    Optional<Site> getSiteIdByURL(String url);

    @Transactional
    @Modifying
    @Query(value = "UPDATE sites AS s SET s.status_time = :ldt WHERE s.id = :id", nativeQuery = true)
    void updateDateTime(int id, LocalDateTime ldt);

    @Transactional
    @Modifying
    @Query(value = "UPDATE sites AS s SET s.status = :status WHERE s.url = :url", nativeQuery = true)
    void changeStatusByUrl(String url, String status);

    @Query(value = "SELECT * FROM search_engine.sites WHERE id = :id", nativeQuery = true)
    Optional<Site> getSiteById(Integer id);

    @Transactional
    @Modifying
    @Query(value = "UPDATE sites AS s SET s.last_error = :message WHERE s.id = :id", nativeQuery = true)
    void setLastError(String message, int id);
}
