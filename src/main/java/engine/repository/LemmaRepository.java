package engine.repository;

import engine.entity.Lemma;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LemmaRepository extends CrudRepository<Lemma,Integer> {
    Integer countBySiteId(Integer siteId);

//    @Query(value="Select * from Lemma Where site_id = :siteId and lemma in (:lemmas)", nativeQuery = true)
//    List<Lemma> findBySiteIdLemmaIn(@Param("siteId") Integer siteId, @Param("lemmas") List<String> lemmas,  Pageable pageable);

    List<Lemma> findBySiteIdAndLemmaIn(Integer siteId, List<String> lemma, Pageable pageable);

}
