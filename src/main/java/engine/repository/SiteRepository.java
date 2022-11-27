package engine.repository;

import engine.entity.Site;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    @Query(value = "Select * from Site\n" +
            " where id in (select distinct Site_Id from page)\n" +
            " order by url", nativeQuery = true)
    List<Site> findSitesFromPageTable();

    Optional<Site> getSiteByUrl(String url);

    @Query(value = "Select url from Site\n" +
            " where id in (select distinct Site_Id from page)\n" +
            " order by url", nativeQuery = true)
    List<String> getSitesUrlFromPageTable(Pageable pageable);

}
