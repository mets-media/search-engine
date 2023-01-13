package engine.repository;

import engine.entity.IndexEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IndexRepository extends CrudRepository<IndexEntity,Integer> {



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
            "    ALTER TABLE IF EXISTS index\n" +
            "    ADD CONSTRAINT FK_LEMMA_CONSTRAINT FOREIGN KEY (lemma_id)\n" +
            "    REFERENCES public.lemma (id) MATCH SIMPLE\n" +
            "    ON UPDATE NO ACTION\n" +
            "    ON DELETE NO ACTION;\n" +
            "\n" +
            "    ALTER TABLE IF EXISTS index\n" +
            "    ADD CONSTRAINT FK_PAGE_CONSTRAINT FOREIGN KEY (page_id)\n" +
            "    REFERENCES public.page (id) MATCH SIMPLE\n" +
            "    ON UPDATE NO ACTION\n" +
            "    ON DELETE NO ACTION;\n", nativeQuery = true)
    void createForeignKeys();

}
