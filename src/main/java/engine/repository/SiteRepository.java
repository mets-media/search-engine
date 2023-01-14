package engine.repository;

import engine.entity.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> getSiteByUrl(String url);

    @Query(value = "Select * from Site " +
            "where id in (select distinct Site_Id from page) " +
            "order by url", nativeQuery = true)
    Page<Site> getSitesFromPageTable(Pageable pageable);

    @Query(value = "Select * from Site " +
            "where id in (select distinct Site_Id from page) " +
            "order by url", nativeQuery = true)
    List<Site> getSitesFromPageTable();

    @Modifying
    @Transactional
    @Query(value="Update Site set page_count = :pageCount Where id = :siteId",nativeQuery = true)
    void setPageCountBySiteId(@Param("siteId") Integer siteId, @Param("pageCount") Integer pageCount);

    @Modifying
    @Transactional
    @Query(value=
            "CREATE OR REPLACE TRIGGER delete_site_trigger\n" +
            "    AFTER DELETE\n" +
            "    ON site\n" +
            "    FOR EACH ROW\n" +
            "    EXECUTE FUNCTION delete_function();",nativeQuery = true)
    void createTrigger();

    @Modifying
    @Transactional
    @Query(value =
            "CREATE OR REPLACE FUNCTION delete_site_information(\n" +
            "siteId integer)\n" +
            "RETURNS integer\n" +
            "LANGUAGE 'plpgsql'\n" +
            "COST 100\n" +
            "VOLATILE PARALLEL UNSAFE\n" +
            "AS $BODY$\n" +
            "begin\n" +
            "delete from keep_link where site_id = siteId;\n" +
            "delete from index where page_id in (select id from page where site_id = siteId);\n" +
            "delete from lemma where site_id = siteId;\n" +
            "delete from page where site_id = siteId;\n" +
            "return siteId;\n" +
            "end;\n" +
            "$BODY$;", nativeQuery = true)
    void createDeleteSiteInfoFunction();

    @Modifying
    @Transactional
    @Query(value = "Select delete_site_information(:siteId)", nativeQuery = true)
    Integer deleteSiteInformation(@Param("siteId") Integer siteId);

    @Query(value =
            "with pages as (select site_id, count(*) page_count from page group by site_id),\n" +
            "     lemmas as (select site_id, count(*) lemma_count from lemma group by site_id ),\n" +
            "    indexes as (select page.site_id, count(*) index_count from index\n" +
            "                 join page on (index.page_id = page.id)\n" +
            "                 group by page.site_id)\n" +
            "    select id, name, url, pages.site_id, pages.page_count, lemmas.lemma_count, indexes.index_count, status, status_time, last_error from pages\n" +
            "    join lemmas on (pages.site_id = lemmas.site_id)\n" +
            "    join indexes on (pages.site_id = indexes.site_id)\n" +
            "    join site on (pages.site_id = site.id)", nativeQuery = true)
    List<Site> getStatistic();

    @Modifying
    @Transactional
    @Query(value =
            "CREATE OR REPLACE FUNCTION get_statistic()\n" +
            "RETURNS TABLE(page_count integer, lemma_count integer, index_count integer)\n" +
            "LANGUAGE 'plpgsql'\n" +
            "COST 100\n" +
            "VOLATILE PARALLEL UNSAFE\n" +
            "ROWS 1 \n" +
            "AS $BODY$\n" +
            "begin\n" +
            "page_count = (select last_value from page_id_seq) - (select last_value - 1 from page_del_count);\n" +
            "lemma_count = (select last_value from lemma_id_seq) - (select last_value - 1 from lemma_del_count) - 1;\n" +
            "index_count = (select last_value from index_id_seq) - (select last_value - 1 from index_del_count);\n" +
	        "return next;\n" +
            "end\n" +
            "$BODY$;\n", nativeQuery = true)
    void creteStatisticFunction();

    @Modifying
    @Transactional
    @Query(value =
            "DO $$DECLARE \n" +
                    "BEGIN\n" +
                    "\t if (not exists (select start_value from pg_sequences where sequencename = 'page_del_count') ) then\n" +
                    "\t\t EXECUTE 'CREATE SEQUENCE Page_Del_Count';\n" +
                    "\t\t perform setval('page_del_count', 1);" +
                    "\t end if;\n" +
                    "\n" +
                    "\t if (not exists (select start_value from pg_sequences where sequencename = 'lemma_del_count') ) then\n" +
                    "\t\t EXECUTE 'CREATE SEQUENCE Lemma_Del_Count';\n" +
                    "\t\t perform setval('lemma_del_count', 1);" +
                    "\t end if;\n" +
                    "\n" +
                    "\t if (not exists (select start_value from pg_sequences where sequencename = 'index_del_count') ) then\n" +
                    "\t\t EXECUTE 'CREATE SEQUENCE Index_Del_Count';\n" +
                    "\t\t perform setval('index_del_count', 1);" +
                    "\t end if;\n" +
                    "END$$;",nativeQuery = true)
    void createSequences();


}
