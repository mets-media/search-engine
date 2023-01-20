package engine.repository;

import engine.entity.SQLContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SQLContentRepository extends JpaRepository<SQLContent, Integer > {
}
