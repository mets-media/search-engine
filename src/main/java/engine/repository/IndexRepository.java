package engine.repository;

import engine.entity.Index;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IndexRepository extends CrudRepository<Index,Integer> {
    @Query(value="Select Page_Id from Index \n" +
            "join Lemma on Lemma.id = Index.Lemma_Id \n" +
            "where Lemma.Lemma = :lemma \n" +
            "and Lemma.Site_Id = :siteId",nativeQuery = true)
    List<Integer> findPageIdByLemmaSiteId(@Param("lemma") String lemma, @Param("siteId") Integer siteId);
}
