package engine.repository;

import engine.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface PageRepository extends JpaRepository<Page, Long> {
   /*
    @Query(value = "Insert into Page (page, content) values (:page, :content)", nativeQuery = true)
    savePage(@Param("page") String page, @Param("content") String content);
   */

    List<Page> findByParentId(Long pageParentId);
    @Transactional
    void deleteByParentId(long pageParentId);
    Optional<Page> findById(Long pageId);
    @Query(value="Select path from page Where parentId = :parentId", nativeQuery = true)
    List<String> findLinksByParentId(@Param("parentId") Long pageParentId);
}

