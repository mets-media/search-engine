package engine.repository;

import engine.entity.KeepLink;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeepLinkRepository extends CrudRepository<KeepLink, Integer> {
    List<String> getPathBySiteId(Integer siteId);
}
