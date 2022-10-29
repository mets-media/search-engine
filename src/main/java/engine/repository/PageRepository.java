package engine.repository;

import engine.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface PageRepository extends JpaRepository<Page, Integer> {
   /*
    @Query(value = "Insert into Page (page, content) values (:page, :content)", nativeQuery = true)
    savePage(@Param("page") String page, @Param("content") String content);
   */

    List<Page> findBySiteId(int pageSiteId);
    @Transactional
    void deleteBySiteId(int pageSiteId);
    Optional<Page> findById(int pageId);
    @Query(value="Select path from page Where site_Id = :siteId", nativeQuery = true)
    List<String> findLinksBySiteId(@Param("siteId") Integer pageSiteId);
    @Query(value="Select count(*) from page where site_Id = :siteId", nativeQuery = true)
    Integer countBySiteId(@Param("siteId") Integer pageSiteId);



}

