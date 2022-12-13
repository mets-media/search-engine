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


    @Query(value=
            "with page_lemma_count as (select lemma_id, count(*) lemma_count, sum(lemma.rank) rank from index \n" +
            "join lemma on (lemma.id = index.lemma_id)\n" +
            "where page_id = :pageId\n" +
            "group by lemma_id)\n" +
            "select lemma_id id, lemma_count frequency, page_lemma_count.rank rank, lemma, lemma.site_id \n" +
            "  from page_lemma_count\n" +
            "  join lemma on (lemma.id = page_lemma_count.lemma_id)\n" +
            "  order by lemma_count desc, lemma\n", nativeQuery = true)
    List<Lemma> findByPageId(@Param("pageId") Integer pageId);
}
