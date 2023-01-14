package engine.repository;

import engine.entity.Lemma;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma,Integer> {


    Integer countBySiteId(Integer siteId);

//    @Query(value="Select * from Lemma Where site_id = :siteId and lemma in (:lemmas)", nativeQuery = true)
//    List<Lemma> findBySiteIdLemmaIn(@Param("siteId") Integer siteId, @Param("lemmas") List<String> lemmas,  Pageable pageable);

    List<Lemma> findBySiteIdAndLemmaIn(Integer siteId, List<String> lemma, Pageable pageable);


    @Query(value = "select 0 id, sum(frequency) frequency, lemma, sum(rank) rank, -1 site_id \n" +
            "from lemma\n" +
            "where lemma in (:lemmaIn)\n" +
            "group by lemma\n" +
            "order by frequency", nativeQuery = true)
    List<Lemma> findByLemmaIn(@Param("lemmaIn") List<String> lemmaIn);


    @Query(value=
            //"with page_lemma_count as (select lemma_id, count(*) lemma_count, sum(lemma.rank) rank from index \n" +
            "with page_lemma_count as (select lemma_id, count(*) lemma_count, sum(index.rank) rank from index \n" +
            "join lemma on (lemma.id = index.lemma_id)\n" +
            "where page_id = :pageId\n" +
            "group by lemma_id)\n" +
            "select lemma_id id, lemma_count frequency, page_lemma_count.rank rank, lemma, lemma.site_id \n" +
            "  from page_lemma_count\n" +
            "  join lemma on (lemma.id = page_lemma_count.lemma_id)\n" +
            "  order by lemma_count desc, lemma\n", nativeQuery = true)
    List<Lemma> findByPageId(@Param("pageId") Integer pageId);

    @Modifying
    @Transactional
    @Query(value = "drop table Lemma;\n" +
            "create table lemma \n" +
            "(id  serial not null, \n" +
            "frequency integer NOT NULL DEFAULT 0,\n" +
            "lemma character varying(255) NOT NULL,\n" +
            "rank real NOT NULL DEFAULT 0,\n" +
            "site_id integer NOT NULL,\n" +
            "CONSTRAINT lemma_pkey PRIMARY KEY (id),\n" +
            "CONSTRAINT siteid_lemma_unique UNIQUE (site_id, lemma)\n" +
            ")",
             nativeQuery = true)
    void reCreateTable();

    @Modifying
    @Transactional
    @Query(value =
            "CREATE or replace TRIGGER lemma_del_trigger\n" +
            "    AFTER DELETE\n" +
            "    ON lemma\n" +
            "    FOR EACH ROW\n" +
            "    EXECUTE FUNCTION delete_function();",nativeQuery = true)
    void createLemmaTrigger();

}
