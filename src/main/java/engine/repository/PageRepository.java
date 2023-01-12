package engine.repository;

import engine.entity.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
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
            "CREATE OR REPLACE FUNCTION insert_page_function()\n" +
                    "RETURNS trigger\n" +
                    "LANGUAGE 'plpgsql'\n" +
                    "COST 100\n" +
                    "VOLATILE NOT LEAKPROOF\n" +
                    "AS $BODY$\n" +
                    "declare siteId integer;\n" +
                    "declare cnt integer;\n" +
                    "begin\n" +
                    "   case tg_table_name\n" +
                    "       when 'page' then\n" +
                    "           begin\n" +
                    "               select page_count from site where id = new.site_id into cnt;\n" +
                    "               update site set page_count = cnt + 1 where id = new.site_id;\n" +
                    "           end;\n" +
                    "       when 'lemma' then\n" +
                    "           begin\n" +
                    "               select lemma_count from site where id = new.site_id into cnt;\n" +
                    "               update site set lemma_count = cnt + 1 where id = new.site_id;\n" +
                    "           end;\n" +
                    "       when 'index' then\n" +
                    "           begin\n" +
                    "               select site_id from page where id = new.page_id into siteId;\n" +
                    "               select index_count from site where id = siteid into cnt;\n" +
                    "               update site set index_count = cnt + 1 where id = siteid;\n" +
                    "           end;\n" +
                    "   end case;\n" +
                    "\n" +
                    "   return null;\n" +
                    "end\n" +
                    "$BODY$;" +
                    "\n" +
                    "CREATE OR REPLACE TRIGGER insert_page_trigger\n" +
                    "    AFTER INSERT\n" +
                    "    ON public.page\n" +
                    "    FOR EACH ROW\n" +
                    "    EXECUTE FUNCTION insert_page_function();" +
                    "\n" +
                    "CREATE OR REPLACE TRIGGER insert_lemma_trigger\n" +
                    "    AFTER INSERT\n" +
                    "    ON lemma\n" +
                    "    FOR EACH ROW\n" +
                    "    EXECUTE FUNCTION insert_page_function();" +

                    "CREATE OR REPLACE TRIGGER insert_index_trigger\n" +
                    "    AFTER INSERT\n" +
                    "    ON index\n" +
                    "    FOR EACH ROW\n" +
                    "    EXECUTE FUNCTION insert_page_function();" +
                    "\n" +
                    "CREATE OR REPLACE FUNCTION public.delete_page_function()\n" +
                    "    RETURNS trigger\n" +
                    "    LANGUAGE 'plpgsql'\n" +
                    "    COST 100\n" +
                    "    VOLATILE NOT LEAKPROOF\n" +
                    "AS $BODY$\n" +
                    "begin\n" +
                    "\tdelete from lemma where id in (Select lemma_id from index where page_id = old.id);\n" +
                    "\tdelete from index where page_id = old.id;\n" +
                    "\n" +
                    "\tupdate site set page_count = (select page_count - 1 from site where id = old.site_id) where id = old.site_id;" +
                    "\n" +
                    "\treturn old;\n" +
                    "end\n" +
                    "$BODY$;\n" +
                    "\n" +
                    "CREATE OR REPLACE TRIGGER delete_page_trigger\n" +
                    "    before DELETE\n" +
                    "    ON page\n" +
                    "    FOR EACH ROW\n" +
                    "    EXECUTE FUNCTION delete_page_function();", nativeQuery = true)
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
}

