package engine.repository;

import engine.entity.Site;
import engine.entity.SiteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    @Query(value = "Select * from Site\n" +
            " where id in (select distinct Site_Id from page)\n" +
            " order by url", nativeQuery = true)
    List<Site> getSitesFromPageTable();

    Optional<Site> getSiteByUrl(String url);

}
