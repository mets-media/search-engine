package engine.repository;

import engine.entity.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
//public interface PageRepository extends PagingAndSortingRepository<Page, Integer> {
    @Transactional
    void deleteBySiteId(int pageSiteId);

    @Query(value = "Select path from page Where site_Id = :siteId", nativeQuery = true)
    List<String> getLinksBySiteId(@Param("siteId") Integer pageSiteId);

    List<Page> findBySiteId(int siteId, Pageable pageable);

    Integer countBySiteId(Integer siteId);

    @Query(value = "select p.id, p.path from page p\n" +
            "join index i on i.page_id = p.id\n" +
            "join lemma l on l.id = i.lemma_id\n" +
            "where l.lemma = :lemma\n" +
            "  and l.site_id = :siteId", nativeQuery = true)
    List<Page> findByLemmaBySiteId(@Param("siteId") Integer siteId, @Param("lemma") String lemma);

    @Query(value = "Select * from page\n" +
            "where id in (select page_id \n" +
            "\t\t\t   from index \n" +
            "\t\t\t   where lemma_id in (Select id \n" +
            "\t\t\t\t\t\t\t\t    from lemma \n" +
            "\t\t\t\t\t\t\t\t    where lemma = :lemma\n" +
            "\t\t\t\t\t\t\t\t      and site_id = :siteId))\n", nativeQuery = true)
    List<Page> findByLemmaSiteId(@Param("siteId") Integer siteId, @Param("lemma") String lemma);

    @Query(value = "Select Path from Page " +
            "join Index on Index.Page_Id = Page.Id " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma and Lemma.Site_Id = :siteId " +
            "order by Path", nativeQuery = true)
    List<String> findPathsBySiteIdLemmaIn(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

    @Query(value = "Select Page.id Page_Id from Page " +
            "join Index on Index.Page_Id = Page.Id " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma and Lemma.Site_Id = :siteId " +
            "order by Path", nativeQuery = true)
    List<Integer> getPageIdBySiteIdAndLemma(@Param("lemma") String lemma, @Param("siteId") Integer siteId);

    @Query(value = "Select Page.id Page_Id from Page " +
            "join Index on Index.Page_Id = Page.Id " +
            "join Lemma on Lemma.Id = Index.Lemma_Id " +
            "where Lemma.Lemma = :lemma " +
            "order by Path", nativeQuery = true)
    List<Integer> getPageIdByLemma(@Param("lemma") String lemma);

    @Query(value = "Select Content from Page where Path = :path", nativeQuery = true)
    String getContentByPath(@Param("path") String path);


    List<Page> findByPathContainingOrderByPath(String filter, Pageable pageable);

    @Modifying
    @Transactional
    void deleteByPath(String path);

    @Modifying
    @Transactional
    @Query(value = "drop table Page;\n" +
            "create table page \n" +
            "(id  serial not null, \n" +
            " code int4 not null, \n" +
            " content Text, \n" +
            " path Text not null, \n" +
            " site_id int4 not null, \n" +
            " primary key (id));\n" +
            " \n" +
            " create index siteId_idx on page (site_id);\n" +
            " \n" +
            " alter table page add constraint siteId_path_unique unique (site_id, path);",
            nativeQuery = true)
    void reCreateTable();

    @Modifying
    @Transactional
    @Query(value =
            "CREATE OR REPLACE FUNCTION delete_function()\n" +
            "RETURNS trigger\n" +
            "LANGUAGE 'plpgsql'\n" +
            "COST 100\n" +
            "VOLATILE NOT LEAKPROOF\n" +
            "AS $BODY$\n" +
            "declare count bigint;\n" +
            "begin\n" +

            "\tcase tg_table_name\n" +
            "\t\twhen 'page' then\n" +
            "\t\t\tbegin\n" +
            "\t\t\t\t------- Счётчик удалений -------\n" +
            "\t\t\t\tselect nextval('page_del_count') into count;\n" +
            "\t\t\t\t--------------------------------\n" +
            "\t\t\t\tdelete from index where page_id = old.id;\n" +
            "\t\t\t\tdelete from lemma where id in (Select lemma_id from index where page_id = old.id);\n" +

            "\t\t\t\tupdate site set page_count = (select page_count - 1 from site where id = old.site_id) where id = old.site_id;\n" +
            "\t\t\tend;\n" +
            "\t\twhen 'lemma' then\n" +
            "\t\t\tbegin\n" +
            "\t\t\t\tselect nextval('lemma_del_count') into count;\n" +
            "\t\t\tend;\n" +
            "\t\twhen 'index' then\n" +
            "\t\t\tbegin\n" +
            "\t\t\t\tselect nextval('index_del_count') into count;\n" +
            "\t\t\tend;\n" +
            "\t\twhen 'site' then\n" +
            "\t\t\tbegin\n" +
            "\t\t\t select count(id) from site into count;\n" +
            "\t\t\t\tif count = 0 then\n" +
            "\t\t\t\tALTER SEQUENCE page_id_seq RESTART WITH 1;\n" +
            "\t\t\t\tALTER SEQUENCE lemma_id_seq RESTART WITH 1;\n" +
            "\t\t\t\tALTER SEQUENCE index_id_seq RESTART WITH 1;\n" +
            "\t\t\t\tALTER SEQUENCE page_del_count RESTART WITH 1;\n" +
            "\t\t\t\tALTER SEQUENCE lemma_del_count RESTART WITH 1;\n" +
            "\t\t\t\tALTER SEQUENCE index_del_count RESTART WITH 1;\n" +
            "\t\t\t\tend if;\n" +
            "\t\t\t\tdelete from keep_link where site_id = old.id;\n" +
            "\t\t\t\tdelete from index where page_id in (select id from page where site_id = old.id);\n" +
            "\t\t\t\tdelete from lemma where site_id = old.id;\n" +
            "\t\t\t\tdelete from page where site_id = old.id;\n" +
            "\t\t\t\t--return null;\n" +
            "\t\tend;\n" +
            "\tend case;\n" +

	        "\treturn old;\n" +
            "end\n" +
            "$BODY$;\n" +
                    "\n" +
                    "CREATE OR REPLACE TRIGGER delete_page_trigger\n" +
                    "    before DELETE\n" +
                    "    ON page\n" +
                    "    FOR EACH ROW\n" +
                    "    EXECUTE FUNCTION delete_function();", nativeQuery = true)
    void createTriggers();

    @Modifying
    @Transactional
    @Query(value = "CREATE OR REPLACE FUNCTION search_lemma_all_sites(includelemma text)\n" +
            "    RETURNS TABLE(page_id integer, abs double precision, rel double precision, path text) \n" +
            "    LANGUAGE 'plpgsql'\n" +
            "    COST 100\n" +
            "    VOLATILE PARALLEL UNSAFE\n" +
            "    ROWS 1000\n" +
            "\n" +
            "AS $BODY$\n" +
            "declare sqlCommand text;\n" +
            "declare sqlText text;\n" +
            "declare new_lemma text;\n" +
            "declare commaCR text;\n" +
            "declare comma text;\n" +
            "declare queryName text;\n" +
            "declare i integer;\n" +
            "declare finalQuery text;\n" +
            "declare lemmas text;\n" +
            "declare stmt text;\n" +
            "declare result_row record;\n" +
            "begin\n" +
            "\tsqlCommand = ' as (select index.page_id, index.rank from index join lemma on (lemma.id = index.lemma_id) where lemma = ';\n" +
            "\tsqlText = '';\tcomma = ''; commaCR = ''; i = 0;\n" +
            "\tqueryName = 'query_';\n" +
            "\tfinalQuery = ''; lemmas = '';\n" +
            "  for new_lemma in (select unnest(string_to_array(includeLemma,',')))\n" +
            "  loop--##########################################################\n" +
            "  \t\tlemmas = lemmas || comma || '''' || new_lemma || '''';\n" +
            "  \n" +
            "  \t\tsqlText = sqlText || commaCR || queryName || i || sqlCommand ||  '''' || new_lemma || ''')';\n" +
            "\t\tcommaCR = ', ' || chr(10); comma = ', '; \n" +
            "\t\tif i > 0 then\n" +
            "\t\t\tfinalQuery = finalQuery || ' join ' || queryName || i || ' on ('|| queryName || 0 || '.page_id = ' || queryName || i || '.page_id)' || chr(10); \n" +
            "\t\tend if;\t\n" +
            "\t\ti = i + 1;\n" +
            "\t\t\n" +
            "  end loop;--#####################################################\n" +
            "  \n" +
            " \n" +
            "  stmt = 'with ' ||  sqlText || ', ' || chr(10) || 'findPages as (select ' || queryName || 0 || '.page_id, ' || queryName || 0 || '.rank from ' || queryName || 0 || chr(10) || \n" +
            "\t\t finalQuery || '), ' || chr(10) || \n" +
            "\t\t 'statisticQuery as (select index.page_id, sum(index.rank) abs, sum(index.rank)/max(index.rank) rel from findPages ' || chr(10) ||\n" +
            "\t\t 'join index on (index.page_id = findPages.page_id) ' || chr(10) ||\n" +
            "\t\t 'where index.lemma_id in (select id lemma_id from lemma where lemma in (' || lemmas || ')) ' || chr(10) || \n" +
            "\t\t 'group by index.page_id) ' || chr(10) ||\n" +
            "\t\t 'select page.id, abs, rel, path from page ' || chr(10) ||\n" +
            "\t\t 'join statisticQuery on (statisticQuery.page_id = page.id) ' || chr(10) ||\n" +
            "\t\t 'order by abs desc, rel asc';\n" +
            "\n" +
            "\t\t for result_row in execute(stmt)\n" +
            "\t\t loop\n" +
            "\t\t   page_id = result_row.id;\n" +
            "\t\t   abs = result_row.abs;\n" +
            "\t\t   rel = result_row.rel;\n" +
            "\t\t   path = result_row.path;\n" +
            "\t\t   return next;\n" +
            "\t\t end loop;\n" +
            "\t\t \n" +
            "end\n" +
            "$BODY$;\n", nativeQuery = true)
    void createFunctionForAllSiteLemmaInfo();

    @Modifying
    @Transactional
    @Query(value =
            "do $$DECLARE\n" +
            "declare max_id integer;\n" +
            "begin\n" +
                    "\tselect max(id) from page into max_id;\n" +
                    "\tif max_id is null then\n" +
                    "\t\t--Alter sequence page_id_seq RESTART WITH 1;\n" +
                    "\t\t--Alter sequence lemma_id_seq RESTART WITH 1;\n" +
                    "\t\t--Alter sequence index_id_seq RESTART WITH 1;\n" +
                    "\t\tAlter sequence page_del_count RESTART WITH 1;\n" +
                    "\t\tAlter sequence lemma_del_count RESTART WITH 1;\n" +
                    "\t\tAlter sequence index_del_count RESTART WITH 1;\n" +
                    "\tend if;\n" +
            "end;$$", nativeQuery = true)
    void resetSequences();

    @Modifying
    @Transactional
    @Query(value =
            "CREATE OR REPLACE FUNCTION reset_counters()\n" +
                    "    RETURNS integer\n" +
                    "    LANGUAGE 'plpgsql'\n" +
                    "    COST 100\n" +
                    "    VOLATILE PARALLEL UNSAFE\n" +
                    "AS $BODY$\n" +
                    "declare max_id integer;\n" +
                    "begin\n" +
                    "\tselect max(id) from page into max_id;\n" +
                    "\tif max_id is null then\n" +
                    "\t\t--Alter sequence page_id_seq RESTART WITH 1;\n" +
                    "\t\t--Alter sequence lemma_id_seq RESTART WITH 1;\n" +
                    "\t\t--Alter sequence index_id_seq RESTART WITH 1;\n" +
                    "\t\tAlter sequence page_del_count RESTART WITH 1;\n" +
                    "\t\tAlter sequence lemma_del_count RESTART WITH 1;\n" +
                    "\t\tAlter sequence index_del_count RESTART WITH 1;\n" +
                    "\t\treturn 1;" +
                    "\tend if;\n" +
                    "\treturn 0;" +
                    "end\n" +
                    "$BODY$;",nativeQuery = true)
    void createFunctionResetCounters();

    @Modifying
    @Transactional
    @Query(value = "select reset_counters();", nativeQuery = true)
    Integer checkForRestartCounters();

}

