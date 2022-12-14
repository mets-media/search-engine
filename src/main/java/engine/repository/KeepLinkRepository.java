package engine.repository;

import engine.entity.KeepLink;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeepLinkRepository extends CrudRepository<KeepLink, Integer> {
}
