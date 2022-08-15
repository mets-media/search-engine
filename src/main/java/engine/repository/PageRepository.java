package engine.repository;

import engine.entity.Page;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PageRepository extends JpaRepository<Page, Long> {
   /*
    @Query(value = "Insert into Page (page, content) values (:page, :content)", nativeQuery = true)
    savePage(@Param("page") String page, @Param("content") String content);
   */

}

