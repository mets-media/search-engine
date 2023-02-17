package engine.repository;

import engine.entity.Lemma;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LemmaRepository extends JpaRepository<Lemma,Integer> {
    List<Lemma> findBySiteIdAndLemmaIn(Integer siteId, List<String> lemma, Pageable pageable);
    List<Lemma> findBySiteIdAndLemmaIn(Integer siteId, List<String> lemma);
    List<Lemma> findByLemmaIn(List<String> lemmas);

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
