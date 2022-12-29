package engine.repository;

import engine.entity.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    @Transactional
    void deleteBySiteId(int pageSiteId);

    @Query(value="Select path from page Where site_Id = :siteId", nativeQuery = true)
    List<String> getLinksBySiteId(@Param("siteId") Integer pageSiteId);

    List<Page> findBySiteId(int siteId, Pageable pageable);

    Integer countBySiteId(Integer siteId);

    @Query(value="select p.id, p.path from page p\n" +
            "join index i on i.page_id = p.id\n" +
            "join lemma l on l.id = i.lemma_id\n" +
            "where l.lemma = :lemma\n" +
            "  and l.site_id = :siteId", nativeQuery = true)
    List<Page> findByLemmaBySiteId(@Param("siteId") Integer siteId, @Param("lemma") String lemma);

    @Query(value="Select * from page\n" +
            "where id in (select page_id \n" +
            "\t\t\t   from index \n" +
            "\t\t\t   where lemma_id in (Select id \n" +
            "\t\t\t\t\t\t\t\t    from lemma \n" +
            "\t\t\t\t\t\t\t\t    where lemma = :lemma\n" +
            "\t\t\t\t\t\t\t\t      and site_id = :siteId))\n",nativeQuery = true)
    List<Page> findByLemmaSiteId(@Param("siteId") Integer siteId, @Param("lemma") String lemma);

    @Query(value="Select Path from Page " +
            "join Index on Index.Page_Id = Page.Id " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma and Lemma.Site_Id = :siteId " +
            "order by Path",nativeQuery = true)
    List<String> findPathsBySiteIdLemmaIn(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

    @Query(value="Select Page.id Page_Id from Page " +
            "join Index on Index.Page_Id = Page.Id " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma and Lemma.Site_Id = :siteId " +
            "order by Path",nativeQuery = true)
    List<Integer> getPageIdBySiteIdLemmaIn(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

    @Query(value = "Select Content from Page where Path = :path", nativeQuery = true)
    String getContentByPath(@Param("path") String path);

}

