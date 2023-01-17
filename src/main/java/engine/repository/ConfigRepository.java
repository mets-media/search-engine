package engine.repository;

import engine.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ConfigRepository extends JpaRepository<Config,Integer> {

    @Transactional
    @Modifying
    @Query(value = "Insert into Config (Id,Key,Name,Value) values " +
            "(-7,'posMs','Позиция отображения сообщений','MIDDLE'), " +
            "(-6,'showT','Время отображения сообщений, м.сек.','10000'), " +
            "(-5,'delay','Пауза при обращении к страницам, м.сек.','0'), " +
            "(-4,'isPoS','Учитывать части речи при индексации','true'), " +
            "(-3,'batch','Размер блока для записи','10'), " +
            //"(-2,'sLiSF','Короткая запись ссылок (boolean)','false'), " +
            "(-1,'tps','Потоков на один сайт (Thread)','8')", nativeQuery = true)
    void initData();

    Optional<Config> findByKey(String key);

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
    @Query(value = "CREATE OR REPLACE FUNCTION get_by_lemma_and_site(\n" +
            "\tlemma_string text,\n" +
            "\tpage_string text,\n" +
            "\tsite_selected integer)\n" +
            "    RETURNS TABLE(page_id integer, abs double precision, rel double precision, path text) \n" +
            "    LANGUAGE 'plpgsql'\n" +
            "    COST 100\n" +
            "    VOLATILE PARALLEL UNSAFE\n" +
            "    ROWS 1000\n" +
            "\n" +
            "AS $BODY$\n" +
            "declare rec record;\n" +
            "declare cur_page_id integer;\n" +
            "declare max_rank float;\n" +
            "declare lemma_site integer;\n" +
            "begin\n" +
            "\tpage_id = 0;\n" +
            "\tabs = 0; rel = 0; max_rank = 0;\n" +
            "\tfor rec in select lemma_id, index.page_id, index.rank from index\n" +
            "\t\t\t\t\t\twhere lemma_id in (select id from lemma where lemma in (select unnest(string_to_array(lemma_string,','))))\n" +
            "\t\t\t\t\t\tand index.page_id in (select cast(unnest(string_to_array(page_string,',')) as integer))\n" +
            "\t\t\t\t\t\torder by page_id\n" +
            "\tloop\n" +
            "\t\tif (rec.page_id != page_id) and (page_id != 0) then\n" +
            "\t\t\trel = abs / max_rank;\n" +
            "\t\t\t\n" +
            "\t\t\tselect page.path from page where id = rec.page_id into path;\n" +
            "\t\t\t\n" +
            "\t\t\tselect lemma.site_id from lemma where lemma.id = rec.lemma_id into lemma_site;\n" +
            "\t\t\t\n" +
            "\t\t\tif site_selected = 0 then \n" +
            "\t\t\t\treturn next;\n" +
            "\t\t\telse\t\n" +
            "\t\t\t--Вывод если сайт совпадает с заданным\n" +
            "\t\t\tif (lemma_site = site_selected) then return next; end if;\n" +
            "\t\t\tend if;\n" +
            "\t\t\t\n" +
            "\t\t\tabs = 0; max_rank = 0; rel = 0;\n" +
            "\t\tend if;\n" +
            "\t\t\n" +
            "\t\tpage_id = rec.page_id;\n" +
            "\t\tabs = abs + rec.rank;\n" +
            "\t\tif max_rank < rec.rank then max_rank = rec.rank; end if;\n" +
            "\t\t\n" +
            "\tend loop;\n" +
            "\t\n" +
            "\tif abs > 0 then \n" +
            "\t\t\trel = abs / max_rank;\n" +
            "\t\t\tselect page.path from page where id = rec.page_id into path;\n" +
            "\t\t\treturn next;\n" +
            "\t\t\tabs = 0; max_rank = 0; rel = 0;\n" +
            "\tend if;\n" +
            "end\n" +
            "$BODY$;", nativeQuery = true)
    void createGetByLemmaAnfSiteIdFunction();


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
    @Query(value = "CREATE OR REPLACE FUNCTION get_pages(\n" +
            "\tlemma_string text,\n" +
            "\tpage_string text,\n" +
            "\tsite_selected integer)\n" +
            "    RETURNS TABLE(page_id integer, abs double precision, rel double precision, path text) \n" +
            "    LANGUAGE 'plpgsql'\n" +
            "    COST 100\n" +
            "    VOLATILE PARALLEL UNSAFE\n" +
            "    ROWS 1000\n" +
            "\n" +
            "AS $BODY$\n" +
            "declare rec_page record;\n" +
            "declare rec_index record;\n" +
            "declare max_rank float;\n" +
            "declare lemmas text[];\n" +
            "\n" +
            "begin\n" +
            "\tpage_id = 0;\t\t\t\t\n" +
            "\t--lemmas = unnest(string_to_array(lemma_string,','));\n" +
            "\n" +
            "\tfor rec_page in select id page_id, page.path\n" +
            "\t\t\t\tfrom page\n" +
            "\t\t\t\twhere id in (select cast(unnest(string_to_array(page_string,',')) as integer))\n" +
            "\t\t\t\t--order by page_id\n" +
            "\tloop\n" +
            "\t\tabs = 0; rel = 0; max_rank = 0;\n" +
            "\t\tfor rec_index in select index.rank\n" +
            "\t\t\t\t\t\t\tfrom index \n" +
            "\t\t\t\t\t\t\twhere index.page_id = rec_page.page_id\n" +
            "\t\t\t\t\t\t\t  and index.lemma_id in (select cast(unnest(string_to_array(lemma_string,',')) as integer))\n" +
            "\t\tloop\n" +
            "\t\t\tabs = abs + rec_index.rank;\n" +
            "\t\t\tif max_rank < rec_index.rank then max_rank = rec_index.rank; end if;\n" +
            "\t\tend loop;\n" +
            "\t\t\n" +
            "\t\tpage_id = rec_page.page_id;\n" +
            "\t\tpath = rec_page.path;\n" +
            "\t\trel = abs / max_rank;\n" +
            "\t\treturn next;" +
            "\t\t\n" +
            "\tend loop;\n" +
            "\t\n" +
            "end\n" +
            "$BODY$;",nativeQuery = true)
    void createGetPagesFunction();
}
