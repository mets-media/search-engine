package engine.repository;

import engine.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
@Repository
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

//    @Transactional
//    default void batchSavePages(List<Page> pages) {
//
//        jdbcTemplate.batchUpdate("Insert nto Page (Site_Id, Path, Code, Content) " +
//                        "VALUES (?, ?, ?, ?)",
//                pages,
//                100,
//                (PreparedStatement ps, Page page) -> {
//                    ps.setInt(1, page.getSiteId());
//                    ps.setString(2, page.getPath());
//                    ps.setInt(3, page.getCode());
//                    ps.setString(4, page.getContent());
//
//                });
//    }



}

