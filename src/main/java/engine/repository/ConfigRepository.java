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
    @Query(value = "Insert into Config (Id,Key,Name,Value, Value_Type) values " +
            "(-7,'posMs','Позиция отображения сообщений','MIDDLE',2), " +
            "(-6,'showT','Время отображения сообщений, м.сек.','3000', 0), " +
            "(-5,'delay','Пауза при обращении к страницам, м.сек.','100',0), " +
            "(-4,'isPoS','Учитывать части речи при индексации','true',1), " +
            "(-3,'batch','Размер блока для записи','10',0)," +
            //"(-2,'sLiSF','Короткая запись ссылок (boolean)','false',1), " +
            "(-1,'tps','Потоков на один сайт (Thread)','8',0)", nativeQuery = true)
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
            """
                    CREATE OR REPLACE FUNCTION reset_counters()
                        RETURNS integer
                        LANGUAGE 'plpgsql'
                        COST 100
                        VOLATILE PARALLEL UNSAFE
                    AS $BODY$
                    declare max_id integer;
                    begin
                    \tselect max(id) from page into max_id;
                    \tif max_id is null then
                    \t\t--Alter sequence page_id_seq RESTART WITH 1;
                    \t\t--Alter sequence lemma_id_seq RESTART WITH 1;
                    \t\t--Alter sequence index_id_seq RESTART WITH 1;
                    \t\tAlter sequence page_del_count RESTART WITH 1;
                    \t\tAlter sequence lemma_del_count RESTART WITH 1;
                    \t\tAlter sequence index_del_count RESTART WITH 1;
                    \t\treturn 1;\tend if;
                    \treturn 0;end
                    $BODY$;""",nativeQuery = true)
    void createFunctionResetCounters();
    @Modifying
    @Transactional
    @Query(value =
            """
                    CREATE OR REPLACE FUNCTION delete_function()
                    RETURNS trigger
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE NOT LEAKPROOF
                    AS $BODY$
                    declare count bigint;
                    begin
                    \tcase tg_table_name
                    \t\twhen 'page' then
                    \t\t\tbegin
                    \t\t\t\t------- Счётчик удалений -------
                    \t\t\t\tselect nextval('page_del_count') into count;
                    \t\t\t\t--------------------------------
                    \t\t\t\tdelete from index where page_id = old.id;
                    \t\t\t\tdelete from lemma where id in (Select lemma_id from index where page_id = old.id);
                    \t\t\t\tupdate site set page_count = (select page_count - 1 from site where id = old.site_id) where id = old.site_id;
                    \t\t\tend;
                    \t\twhen 'lemma' then
                    \t\t\tbegin
                    \t\t\t\tselect nextval('lemma_del_count') into count;
                    \t\t\tend;
                    \t\twhen 'index' then
                    \t\t\tbegin
                    \t\t\t\tselect nextval('index_del_count') into count;
                    \t\t\tend;
                    \t\twhen 'site' then
                    \t\t\tbegin
                    \t\t\t select count(id) from site into count;
                    \t\t\t\tif count = 0 then
                    \t\t\t\tALTER SEQUENCE page_id_seq RESTART WITH 1;
                    \t\t\t\tALTER SEQUENCE lemma_id_seq RESTART WITH 1;
                    \t\t\t\tALTER SEQUENCE index_id_seq RESTART WITH 1;
                    \t\t\t\tALTER SEQUENCE page_del_count RESTART WITH 1;
                    \t\t\t\tALTER SEQUENCE lemma_del_count RESTART WITH 1;
                    \t\t\t\tALTER SEQUENCE index_del_count RESTART WITH 1;
                    \t\t\t\tend if;
                    \t\t\t\tdelete from keep_link where site_id = old.id;
                    \t\t\t\tdelete from index where page_id in (select id from page where site_id = old.id);
                    \t\t\t\tdelete from lemma where site_id = old.id;
                    \t\t\t\tdelete from page where site_id = old.id;
                    \t\t\t\t--return null;
                    \t\tend;
                    \tend case;
                    \treturn old;
                    end
                    $BODY$;

                    CREATE OR REPLACE TRIGGER delete_page_trigger
                        before DELETE
                        ON page
                        FOR EACH ROW
                        EXECUTE FUNCTION delete_function();""", nativeQuery = true)
    void createTriggers();

    @Modifying
    @Transactional
    //@Query(value = "CREATE OR REPLACE FUNCTION search_lemma_all_sites(includelemma text)\n" +
    @Query(value = """
            CREATE OR REPLACE FUNCTION get_pages_generate_stmt(includelemma text)
                RETURNS TABLE(page_id integer, abs double precision, rel double precision, path text)\s
                LANGUAGE 'plpgsql'
                COST 100
                VOLATILE PARALLEL UNSAFE
                ROWS 1000

            AS $BODY$
            declare sqlCommand text;
            declare sqlText text;
            declare new_lemma text;
            declare commaCR text;
            declare comma text;
            declare queryName text;
            declare i integer;
            declare finalQuery text;
            declare lemmas text;
            declare stmt text;
            declare result_row record;
            begin
            \tsqlCommand = ' as (select index.page_id, index.rank from index join lemma on (lemma.id = index.lemma_id) where lemma = ';
            \tsqlText = '';\tcomma = ''; commaCR = ''; i = 0;
            \tqueryName = 'query_';
            \tfinalQuery = ''; lemmas = '';
              for new_lemma in (select unnest(string_to_array(includeLemma,',')))
              loop--##########################################################
              \t\tlemmas = lemmas || comma || '''' || new_lemma || '''';
             \s
              \t\tsqlText = sqlText || commaCR || queryName || i || sqlCommand ||  '''' || new_lemma || ''')';
            \t\tcommaCR = ', ' || chr(10); comma = ', ';\s
            \t\tif i > 0 then
            \t\t\tfinalQuery = finalQuery || ' join ' || queryName || i || ' on ('|| queryName || 0 || '.page_id = ' || queryName || i || '.page_id)' || chr(10);\s
            \t\tend if;\t
            \t\ti = i + 1;
            \t\t
              end loop;--#####################################################
             \s
            \s
              stmt = 'with ' ||  sqlText || ', ' || chr(10) || 'findPages as (select ' || queryName || 0 || '.page_id, ' || queryName || 0 || '.rank from ' || queryName || 0 || chr(10) ||\s
            \t\t finalQuery || '), ' || chr(10) ||\s
            \t\t 'statisticQuery as (select index.page_id, sum(index.rank) abs, sum(index.rank)/max(index.rank) rel from findPages ' || chr(10) ||
            \t\t 'join index on (index.page_id = findPages.page_id) ' || chr(10) ||
            \t\t 'where index.lemma_id in (select id lemma_id from lemma where lemma in (' || lemmas || ')) ' || chr(10) ||\s
            \t\t 'group by index.page_id) ' || chr(10) ||
            \t\t 'select page.id, abs, rel, path from page ' || chr(10) ||
            \t\t 'join statisticQuery on (statisticQuery.page_id = page.id) ' || chr(10) ||
            \t\t 'order by abs desc, rel desc';

            \t\t for result_row in execute(stmt)
            \t\t loop
            \t\t   page_id = result_row.id;
            \t\t   abs = result_row.abs;
            \t\t   rel = result_row.rel;
            \t\t   path = result_row.path;
            \t\t   return next;
            \t\t end loop;
            \t\t\s
            end
            $BODY$;
            """, nativeQuery = true)
    void createFunctionForAllSiteLemmaInfo();


    @Modifying
    @Transactional
    @Query(value =
            """
                    CREATE OR REPLACE FUNCTION get_pages_index_page_lemma(
                    \tlemmas_id text,
                    \tpages_id text,
                    \tsite_selected integer)
                        RETURNS TABLE(page_id integer, abs double precision, rel double precision, path text)\s
                        LANGUAGE 'plpgsql'
                        COST 100
                        VOLATILE PARALLEL UNSAFE
                        ROWS 1000

                    AS $BODY$
                    declare rec record;
                    declare cur_page_id integer;
                    declare max_rank float;
                    declare lemma_site integer;
                    begin
                    \tpage_id = 0;
                    \tabs = 0; rel = 0; max_rank = 0;
                    \tfor rec in select lemma_id, index.page_id, index.rank from index
                    \t\t\t\t\t\twhere lemma_id in (select cast(unnest(string_to_array(lemmas_id,',')) as integer))
                    \t\t\t\t\t\tand index.page_id in (select cast(unnest(string_to_array(pages_id,',')) as integer))
                    \t\t\t\t\t\t--order by page_id
                    \tloop
                    \t\tif (rec.page_id != page_id) and (page_id != 0) then
                    \t\t\trel = abs / max_rank;
                    \t\t\t
                    \t\t\tselect page.path from page where id = rec.page_id into path;
                    \t\t\t
                    \t\t\tselect lemma.site_id from lemma where lemma.id = rec.lemma_id into lemma_site;
                    \t\t\t
                    \t\t\tif site_selected = 0 then\s
                    \t\t\t\treturn next;
                    \t\t\telse\t
                    \t\t\t--Вывод если сайт совпадает с заданным
                    \t\t\tif (lemma_site = site_selected) then return next; end if;
                    \t\t\tend if;
                    \t\t\t
                    \t\t\tabs = 0; max_rank = 0; rel = 0;
                    \t\tend if;
                    \t\t
                    \t\tpage_id = rec.page_id;
                    \t\tabs = abs + rec.rank;
                    \t\tif max_rank < rec.rank then max_rank = rec.rank; end if;
                    \t\t
                    \tend loop;
                    \t
                    \tif abs > 0 then\s
                    \t\t\trel = abs / max_rank;
                    \t\t\tselect page.path from page where id = rec.page_id into path;
                    \t\t\treturn next;
                    \t\t\tabs = 0; max_rank = 0; rel = 0;
                    \tend if;
                    end
                    $BODY$;""", nativeQuery = true)
    void createGetByLemmaAnfSiteIdFunction();


    @Modifying
    @Transactional
    @Query(value =
            """
                    do $$DECLARE
                    declare max_id integer;
                    begin
                    \tselect max(id) from page into max_id;
                    \tif max_id is null then
                    \t\t--Alter sequence page_id_seq RESTART WITH 1;
                    \t\t--Alter sequence lemma_id_seq RESTART WITH 1;
                    \t\t--Alter sequence index_id_seq RESTART WITH 1;
                    \t\tAlter sequence page_del_count RESTART WITH 1;
                    \t\tAlter sequence lemma_del_count RESTART WITH 1;
                    \t\tAlter sequence index_del_count RESTART WITH 1;
                    \tend if;
                    end;$$""", nativeQuery = true)
    void resetSequences();
    @Modifying
    @Transactional
    @Query(value = """
            CREATE OR REPLACE FUNCTION get_pages_page_index(
            \tlemma_id_array text,
            \tpage_Id_array text
            )--\t,site_selected integer)
                RETURNS TABLE(page_id integer, abs double precision, rel double precision, path text)\s
                LANGUAGE 'plpgsql'
                COST 100
                VOLATILE PARALLEL UNSAFE
                ROWS 1000

            AS $BODY$
            declare rec_page record;
            declare rec_index record;
            declare max_rank float;
            declare lemmas text[];

            begin
            \tpage_id = 0;\t\t\t\t
            \t--lemmas = unnest(string_to_array(lemma_string,','));

            \tfor rec_page in select id page_id, page.path
            \t\t\t\tfrom page
            \t\t\t\twhere id in (select cast(unnest(string_to_array(page_id_array,',')) as integer))
            \t\t\t\t--order by page_id
            \tloop
            \t\tabs = 0; rel = 0; max_rank = 0;

            \t\tselect sum(rank) abs, max(rank) max_rank\s
            \t\tfrom index\s
            \t\twhere index.page_id = rec_page.page_id
            \t\t  and index.lemma_id in (select cast(unnest(string_to_array(lemma_id_array,',')) as integer))
            \t\tinto abs, max_rank; \s
            \t\tpage_id = rec_page.page_id;
            \t\tpath = rec_page.path;
            \t\trel = abs / max_rank;
            \t\treturn next;\t\t
            \tend loop;
            \t
            end
            $BODY$;""",nativeQuery = true)
    void createGetPagesFunction();
}
