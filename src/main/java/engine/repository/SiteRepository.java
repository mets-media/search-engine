package engine.repository;

import engine.entity.PathTable;
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

    @Query(value =
            "Select * from Site \n" +
            "where id in (select distinct Site_Id from page) \n" +
            "order by id", nativeQuery = true)
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


    /*
    id integer, name text, url text, page_count integer, lemma_count integer, index_count integer,
    status bytea, status_time timestamp without time zone, last_error text
     */
    @Modifying
    @Transactional
    @Query(value =
            "CREATE OR REPLACE FUNCTION get_counters()\n" +
            "RETURNS TABLE(id integer, name text, url text, " +
                    "page_count integer, lemma_count integer, index_count integer, \n" +
                    "status bytea, status_time timestamp without time zone, last_error text) \n" +
            "LANGUAGE 'plpgsql'\n" +
            "COST 100\n" +
            "VOLATILE PARALLEL UNSAFE\n" +
            "ROWS 1 \n" +
            "AS $BODY$\n" +
            "begin \n" +
            "\tid = 0; name = '*'; url = 'Все сайты'; status = 'NEW_SITE'; status_time = now(); last_error = '';" +
            "\n" +
            "\tpage_count =  (select last_value from page_id_seq)  - (select last_value - 1 from page_del_count);\n" +
            "\tlemma_count = (select last_value from lemma_id_seq) - (select last_value - 1 from lemma_del_count);\n" +
            "\tindex_count = (select last_value from index_id_seq) - (select last_value - 1 from index_del_count);\n" +
	        "\treturn next;\n" +
            "end\n" +
            "$BODY$;\n", nativeQuery = true)
    void creteGetCountersFunction();

    @Query(value =
            "select 0 id, '*' name, 'Все сайты' url,\n" +
                    "(select last_value from page_id_seq)  - (select last_value - 1 from page_del_count) page_count,\n" +
                    "(select last_value from lemma_id_seq) - (select last_value - 1 from lemma_del_count) lemma_count,\n" +
                    "(select last_value from index_id_seq) - (select last_value - 1 from index_del_count) index_count,\n" +
                    "null status, now() status_time, '' last_error \n" +
                    "from one_record_table;\n",
            nativeQuery = true)
    Optional<Site> getAllSiteInfo();

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

    @Modifying
    @Transactional
    @Query(value ="CREATE TABLE IF NOT EXISTS one_record_table\n" +
            "(\n" +
            "    id integer NOT NULL,\n" +
            "    CONSTRAINT one_record_table_pkey PRIMARY KEY (id)\n" +
            ");\n" +
            "insert into one_record_table (id) values (0) on conflict do nothing;\n", nativeQuery = true)
    void createOneRecordTable();

    @Query(value = "select * from get_by_lemma_and_site(:lemmas, :siteId)",nativeQuery = true)
    List<PathTable> getResultByLemmasAndSiteId(@Param("lemmas") String lemmas, @Param("siteId") Integer siteId);

/*
DO $$declare
    declare record Record;
    declare page_count_total integer;
    declare lemma_count_total integer;
    declare index_count_total integer;

    declare page_ integer;
    declare lemma_ integer;
    declare index_ integer;

begin
	page_count_total = 0;
	lemma_count_total = 0;
	index_count_total = 0;

	for record in
	(with pages as (select site_id, count(*) page_count from page group by site_id),
		lemmas as (select site_id, count(*) lemma_count from lemma group by site_id ),
    	indexes as (select page.site_id, count(*) index_count from index
    	join page on (index.page_id = page.id)
    	group by page.site_id)
    	select id, name, url, pages.site_id, pages.page_count, lemmas.lemma_count, indexes.index_count, status, status_time, last_error from pages
    	join lemmas on (pages.site_id = lemmas.site_id)
    	join indexes on (pages.site_id = indexes.site_id)
    	join site on (pages.site_id = site.id)
	)
    loop
    	update site
    	set page_count = record.page_count,
            lemma_count= record.lemma_count,
            index_count = record.index_count
    	where site.id = record.id;
		page_count_total = page_count_total + record.page_count;
		lemma_count_total = lemma_count_total + record.lemma_count;
		index_count_total = index_count_total + record.index_count;
	end loop;

	select last_value - page_count_total from page_id_seq into page_;
	select last_value - lemma_count_total from lemma_id_seq into lemma_;
	select last_value - index_count_total from index_id_seq into index_;

	perform setval('page_del_count', page_ + 1);
	perform setval('lemma_del_count', lemma_ + 1);
	perform setval('index_del_count', index_ + 1);


end;$$
*/
}
