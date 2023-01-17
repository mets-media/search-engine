package engine.repository;

import engine.entity.IndexEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IndexRepository extends CrudRepository<IndexEntity, Integer> {


    @Query(value = "select count(*) from index\n" +
            "join page on (index.page_id = page.id)\n" +
            "where page.site_id = :siteId", nativeQuery = true)
    Integer countBySiteId(@Param("siteId") Integer siteId);

    //Integer countBySiteId(Integer siteId);
    @Query(value = "Select Page_Id from Index \n" +
            "join Lemma on Lemma.id = Index.Lemma_Id \n" +
            "where Lemma.Lemma = :lemma \n" +
            "and Lemma.Site_Id = :siteId", nativeQuery = true)
    List<Integer> findPageIdByLemmaSiteId(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

    @Modifying
    @Transactional
    @Query(value = "drop table index;\n" +
            "create table index\n" +
            "(id serial not null, \n" +
            "lemma_id integer NOT NULL,\n" +
            "page_id integer NOT NULL,\n" +
            "rank real NOT NULL,\n" +
            "CONSTRAINT index_pkey PRIMARY KEY (id)\n" +
            ")", nativeQuery = true)
    void reCreateTable();

    @Modifying
    @Transactional
    @Query(value =
            "DO $$DECLARE \n" +
                    "BEGIN\n" +
                    "\t if (not exists (select oid from pg_constraint where conname = 'fk_lemma_constraint') ) then\n" +
                    "\t\t EXECUTE 'ALTER TABLE IF EXISTS index \n" +
                    "    \t ADD CONSTRAINT FK_LEMMA_CONSTRAINT FOREIGN KEY (lemma_id) \n" +
                    "\t     REFERENCES public.lemma (id) MATCH SIMPLE \n" +
                    "    \t ON UPDATE NO ACTION \n" +
                    "\t     ON DELETE NO ACTION';\n" +
                    "\t end if;\n" +
                    "\n" +
                    "\t if (not exists (select oid from pg_constraint where conname = 'fk_page_constraint') ) then\n" +
                    "\t\t EXECUTE 'ALTER TABLE IF EXISTS index  \n" +
                    "            ADD CONSTRAINT FK_PAGE_CONSTRAINT FOREIGN KEY (page_id) \n" +
                    "            REFERENCES public.page (id) MATCH SIMPLE \n" +
                    "            ON UPDATE NO ACTION \n" +
                    "            ON DELETE NO ACTION';\n" +
                    "\t end if;\n" +
                    "END$$;", nativeQuery = true)
    void createForeignKeys();

    @Modifying
    @Transactional
    @Query(value = "CREATE or replace TRIGGER index_del_trigger\n" +
            "    AFTER DELETE\n" +
            "    ON index\n" +
            "    FOR EACH ROW\n" +
            "    EXECUTE FUNCTION delete_function();",nativeQuery = true)
    void createIndexTrigger();

    @Query(value = "Select * from index " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma ", nativeQuery = true)
    List<IndexEntity> getIndexByLemmaForAllSites(@Param("lemma") String lemma);

    @Query(value = "Select * from index " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma " +
            "  and site_id = :siteId", nativeQuery = true)
    List<IndexEntity> getIndexByLemmaForSiteId(@Param("lemma") String lemma, @Param("siteId") int siteId);

}
