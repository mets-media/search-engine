package engine.repository;

import engine.entity.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> getSiteByUrl(String url);

    @Query(value = "Select * from Site " +
            "where id in (select distinct Site_Id from page) " +
            "order by url", nativeQuery = true)
    Page<Site> getSitesUrlFromPageTable(Pageable pageable);

}
