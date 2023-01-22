package engine.repository;

import engine.entity.IndexEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IndexRepository extends CrudRepository<IndexEntity, Integer> {


    @Query(value = """
            select count(*) from index
            join page on (index.page_id = page.id)
            where page.site_id = :siteId""", nativeQuery = true)
    Integer countBySiteId(@Param("siteId") Integer siteId);

    @Query(value = """
            Select Page_Id from Index\s
            join Lemma on Lemma.id = Index.Lemma_Id\s
            where Lemma.Lemma = :lemma\s
            and Lemma.Site_Id = :siteId""", nativeQuery = true)
    List<Integer> findPageIdByLemmaSiteId(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

    @Modifying
    @Transactional
    @Query(value = """
            drop table index;
            create table index
            (id serial not null,\s
            lemma_id integer NOT NULL,
            page_id integer NOT NULL,
            rank real NOT NULL,
            CONSTRAINT index_pkey PRIMARY KEY (id)
            )""", nativeQuery = true)
    void reCreateTable();

    @Modifying
    @Transactional
    @Query(value =
            """
                    DO $$DECLARE 
                    BEGIN
                      if (not exists (select oid from pg_constraint where conname = 'fk_lemma_constraint') ) then
                       EXECUTE 'ALTER TABLE IF EXISTS index 
                          ADD CONSTRAINT FK_LEMMA_CONSTRAINT FOREIGN KEY (lemma_id) 
                          REFERENCES public.lemma (id) MATCH SIMPLE 
                          ON UPDATE NO ACTION 
                          ON DELETE NO ACTION';
                      end if;

                      if (not exists (select oid from pg_constraint where conname = 'fk_page_constraint') ) then
                         EXECUTE 'ALTER TABLE IF EXISTS index 
                                ADD CONSTRAINT FK_PAGE_CONSTRAINT FOREIGN KEY (page_id) 
                                REFERENCES public.page (id) MATCH SIMPLE 
                                ON UPDATE NO ACTION 
                                ON DELETE NO ACTION';
                       end if;
                    END$$;""", nativeQuery = true)
    void createForeignKeys();

    @Modifying
    @Transactional
    @Query(value = """
            CREATE or replace TRIGGER index_del_trigger
                AFTER DELETE
                ON index
                FOR EACH ROW
                EXECUTE FUNCTION delete_function();""",nativeQuery = true)
    void createIndexTrigger();

    @Query(value = "Select * from index " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma " +
            "", nativeQuery = true)
    List<IndexEntity> getIndexByLemmaForAllSites(@Param("lemma") String lemma);

    @Query(value = "Select * from index " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma " +
            "  and site_id = :siteId", nativeQuery = true)
    List<IndexEntity> getIndexByLemmaForSiteId(@Param("lemma") String lemma, @Param("siteId") int siteId);

    //для выбранного сайта lemmaIn содержит список lemma_id
    @Query(value = "select * from index where page_Id = :pageId and lemma_id in (:lemmaIn)", nativeQuery = true)
    List<IndexEntity> findByPageIdLemmaIdIn(@Param("pageId") Integer pageId, @Param("lemmaIn") List<Integer> lemmaIn);

}
