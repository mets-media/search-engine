package engine.repository;

import engine.entity.Index;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IndexRepository extends CrudRepository<Index,Integer> {
    @Query(value = "select count(*) from index\n" +
            "join page on (index.page_id = page.id)\n" +
            "where page.site_id = :siteId", nativeQuery = true)
    Integer countBySiteId(@Param("siteId") Integer siteId);

    //Integer countBySiteId(Integer siteId);
    @Query(value="Select Page_Id from Index \n" +
            "join Lemma on Lemma.id = Index.Lemma_Id \n" +
            "where Lemma.Lemma = :lemma \n" +
            "and Lemma.Site_Id = :siteId",nativeQuery = true)
    List<Integer> findPageIdByLemmaSiteId(@Param("lemma") String lemma, @Param("siteId") Integer siteId);
}
