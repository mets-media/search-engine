package engine.repository;

import engine.entity.Site;
import engine.entity.SiteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
}
