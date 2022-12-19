package engine.repository;

import engine.entity.KeepLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface KeepLinkRepository extends JpaRepository<KeepLink, Integer> {
    @Query(value="Select path from Keep_Link Where Site_Id = :siteId", nativeQuery = true)
    List<String> getPathsBySiteId(@Param("siteId") Integer siteId);

    @Transactional
    @Modifying
    void deleteBySiteId(Integer siteId);

}
