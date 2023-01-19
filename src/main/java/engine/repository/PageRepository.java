package engine.repository;

import engine.entity.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
//public interface PageRepository extends PagingAndSortingRepository<Page, Integer> {
    @Transactional
    void deleteBySiteId(int pageSiteId);

    @Query(value = "Select path from page Where site_Id = :siteId", nativeQuery = true)
    List<String> getLinksBySiteId(@Param("siteId") Integer pageSiteId);

    List<Page> findBySiteId(int siteId, Pageable pageable);

    Integer countBySiteId(Integer siteId);

    @Query(value = "Select Page.id Page_Id from Page " +
            "join Index on Index.Page_Id = Page.Id " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma and Lemma.Site_Id = :siteId " +
            "order by Path", nativeQuery = true)
    List<Integer> getPageIdBySiteIdAndLemma(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

    @Query(value = "Select Page.id Page_Id from Page " +
            "join Index on Index.Page_Id = Page.Id " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma " +
            "order by Path", nativeQuery = true)
    List<Integer> getPageIdByLemma(@Param("lemma") String lemma);

    @Query(value = "Select Content from Page where Path = :path", nativeQuery = true)
    String getContentByPath(@Param("path") String path);


    List<Page> findByPathContainingOrderByPath(String filter, Pageable pageable);

    @Modifying
    @Transactional
    void deleteByPath(String path);

    @Modifying
    @Transactional
    @Query(value = """
            drop table Page;
            create table page\s
            (id  serial not null,\s
             code int4 not null,\s
             content Text,\s
             path Text not null,\s
             site_id int4 not null,\s
             primary key (id));
            \s
             create index siteId_idx on page (site_id);
            \s
             alter table page add constraint siteId_path_unique unique (site_id, path);""",
            nativeQuery = true)
    void reCreateTable();

    @Modifying
    @Transactional
    @Query(value = "select reset_counters();", nativeQuery = true)
    Integer checkForRestartCounters();


}

