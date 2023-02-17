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
            "(-2,'T.out','Timeout при загрузке страниц','2000',0)," +
            "(-1,'tps','Потоков на один сайт (Thread)','4',0)", nativeQuery = true)
    void initData();

    Optional<Config> findByKey(String key);

    @Modifying
    @Transactional
    @Query(value =
            """
                    DO $$DECLARE
                    BEGIN
                       if (not exists (select start_value from pg_sequences where sequencename = 'page_del_count') ) then
                          EXECUTE 'CREATE SEQUENCE Page_Del_Count';
                          perform setval('page_del_count', 1);
                       end if;

                       if (not exists (select start_value from pg_sequences where sequencename = 'lemma_del_count') ) then
                          EXECUTE 'CREATE SEQUENCE Lemma_Del_Count';
                          perform setval('lemma_del_count', 1);
                       end if;

                       if (not exists (select start_value from pg_sequences where sequencename = 'index_del_count') ) then
                         EXECUTE 'CREATE SEQUENCE Index_Del_Count';
                         perform setval('index_del_count', 1);
                       end if;
                    END$$;""",nativeQuery = true)
    void createSequences();

    @Modifying
    @Transactional
    @Query(value = """
            CREATE TABLE IF NOT EXISTS one_record_table
            (
                id integer NOT NULL,
                CONSTRAINT one_record_table_pkey PRIMARY KEY (id)
            );
            insert into one_record_table (id) values (0) on conflict do nothing;
            """, nativeQuery = true)
    void createOneRecordTable();

    @Modifying
    @Transactional
    @Query(value =
            """
                    CREATE OR REPLACE FUNCTION get_counters()
                    RETURNS TABLE(id integer, name text, url text, page_count integer, lemma_count integer, index_count integer,
                    status bytea, status_time timestamp without time zone, last_error text)
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE PARALLEL UNSAFE
                    ROWS 1
                    AS $BODY$
                    begin
                      id = 0; name = '*'; url = 'Все сайты'; status = 'NEW_SITE'; status_time = now(); last_error = '';
                      page_count =  (select last_value from page_id_seq)  - (select last_value - 1 from page_del_count);
                      lemma_count = (select last_value from lemma_id_seq) - (select last_value - 1 from lemma_del_count);
                      index_count = (select last_value from index_id_seq) - (select last_value - 1 from index_del_count);
                      return next;
                    end
                    $BODY$;
                    """, nativeQuery = true)
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
            """
                    CREATE OR REPLACE FUNCTION delete_site_information(
                    siteId integer)
                    RETURNS integer
                    LANGUAGE 'plpgsql'
                    COST 100
                    VOLATILE PARALLEL UNSAFE
                    AS $BODY$
                    begin
                    delete from keep_link where site_id = siteId;
                    delete from index where page_id in (select id from page where site_id = siteId);
                    delete from lemma where site_id = siteId;
                    delete from page where site_id = siteId;
                    return siteId;
                    end;
                    $BODY$;""", nativeQuery = true)
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
                      case tg_table_name
                       when 'page' then
                        begin
                         ------- Счётчик удалений -------
                         select nextval('page_del_count') into count;
                         --------------------------------
                         delete from index where page_id = old.id;
                         delete from lemma where id in (Select lemma_id from index where page_id = old.id);
                         update site set page_count = (select page_count - 1 from site where id = old.site_id) where id = old.site_id;
                         end;
                       when 'lemma' then
                        begin
                         select nextval('lemma_del_count') into count;
                        end;
                       when 'index' then
                        begin
                         select nextval('index_del_count') into count;
                        end;
                       when 'site' then
                        begin
                         select count(id) from site into count;
                         
                         if count = 0 then
                          ALTER SEQUENCE page_id_seq RESTART WITH 1;
                          ALTER SEQUENCE lemma_id_seq RESTART WITH 1;
                          ALTER SEQUENCE index_id_seq RESTART WITH 1;
                          ALTER SEQUENCE page_del_count RESTART WITH 1;
                          ALTER SEQUENCE lemma_del_count RESTART WITH 1;
                          ALTER SEQUENCE index_del_count RESTART WITH 1;
                         end if;
                         
                         delete from keep_link where site_id = old.id;
                         delete from index where page_id in (select id from page where site_id = old.id);
                         delete from lemma where site_id = old.id;
                         delete from page where site_id = old.id;
                         --return null;
                        end;
                      end case;
                      return old;
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
                sqlCommand = ' as (select index.page_id, index.rank from index join lemma on (lemma.id = index.lemma_id) where lemma = ';
                sqlText = ''; comma = ''; commaCR = ''; i = 0;
                queryName = 'query_';
                finalQuery = ''; lemmas = '';
              for new_lemma in (select unnest(string_to_array(includeLemma,',')))
              loop--##########################################################
                    lemmas = lemmas || comma || '''' || new_lemma || '''';
            
                    sqlText = sqlText || commaCR || queryName || i || sqlCommand ||  '''' || new_lemma || ''')';
                    commaCR = ', ' || chr(10); comma = ', ';
                    if i > 0 then
                        finalQuery = finalQuery || ' join ' || queryName || i || ' on ('|| queryName || 0 || '.page_id = ' || queryName || i || '.page_id)' || chr(10);\s
                    end if;
                    i = i + 1;
            
              end loop;--#####################################################
            
            
              stmt = 'with ' ||  sqlText || ', ' || chr(10) || 'findPages as (select ' || queryName || 0 || '.page_id, ' || queryName || 0 || '.rank from ' || queryName || 0 || chr(10) ||\s
                     finalQuery || '), ' || chr(10) || 
                     'statisticQuery as (select index.page_id, sum(index.rank) abs, sum(index.rank)/max(index.rank) rel from findPages ' || chr(10) ||
                     'join index on (index.page_id = findPages.page_id) ' || chr(10) ||
                     'where index.lemma_id in (select id lemma_id from lemma where lemma in (' || lemmas || ')) ' || chr(10) ||\s
                     'group by index.page_id) ' || chr(10) ||
                     'select page.id, abs, rel, path from page ' || chr(10) ||
                     'join statisticQuery on (statisticQuery.page_id = page.id) ' || chr(10) ||
                     'order by abs desc, rel desc';

                 for result_row in execute(stmt)
                 loop
                   page_id = result_row.id;
                   abs = result_row.abs;
                   rel = result_row.rel;
                   path = result_row.path;
                   return next;
                 end loop;
            end
            $BODY$;
            """, nativeQuery = true)
    void createFunctionForAllSiteLemmaInfo();

    @Modifying
    @Transactional
    @Query(value =
            """
                    CREATE OR REPLACE FUNCTION get_pages_index_page_lemma(
                        lemmas_id text,
                        pages_id text,
                        site_selected integer)
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
                        page_id = 0;
                        abs = 0; rel = 0; max_rank = 0;
                        for rec in select lemma_id, index.page_id, index.rank from index
                                    where lemma_id in (select cast(unnest(string_to_array(lemmas_id,',')) as integer))
                                      and index.page_id in (select cast(unnest(string_to_array(pages_id,',')) as integer))
                                      order by page_id
                        loop
                            if (rec.page_id != page_id) and (page_id != 0) then
                                rel = abs / max_rank;
                    
                                select page.path from page where id = rec.page_id into path;
                    
                                select lemma.site_id from lemma where lemma.id = rec.lemma_id into lemma_site;
                    
                                if site_selected = 0 then 
                                    return next;
                                else 
                                --Вывод если сайт совпадает с заданным
                                if (lemma_site = site_selected) then return next; end if;
                                end if;
                    
                                abs = 0; max_rank = 0; rel = 0;
                            end if;
                    
                            page_id = rec.page_id;
                            abs = abs + rec.rank;
                            if max_rank < rec.rank then max_rank = rec.rank; end if;
                    
                        end loop;
                    
                        if abs > 0 then 
                            rel = abs / max_rank;
                            select page.path from page where id = rec.page_id into path;
                            return next;
                            abs = 0; max_rank = 0; rel = 0;
                        end if;
                    end
                    $BODY$;""", nativeQuery = true)
    void createGetByLemmaAndSiteIdFunction();


    @Modifying
    @Transactional
    @Query(value =
            """
                    do $$DECLARE
                    declare max_id integer;
                    begin
                        select max(id) from page into max_id;
                        if max_id is null then
                            --Alter sequence page_id_seq RESTART WITH 1;
                            --Alter sequence lemma_id_seq RESTART WITH 1;
                            --Alter sequence index_id_seq RESTART WITH 1;
                            Alter sequence page_del_count RESTART WITH 1;
                            Alter sequence lemma_del_count RESTART WITH 1;
                            Alter sequence index_del_count RESTART WITH 1;
                        end if;
                    end;$$""", nativeQuery = true)
    void resetSequences();
    @Modifying
    @Transactional
    @Query(value = """
            CREATE OR REPLACE FUNCTION get_pages_page_index(
                lemma_id_array text,
                page_Id_array text
            )
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
                page_id = 0;
                --lemmas = unnest(string_to_array(lemma_string,','));

                for rec_page in select id page_id, page.path
                                  from page
                                  where id in (select cast(unnest(string_to_array(page_id_array,',')) as integer))
                loop
                    abs = 0; rel = 0; max_rank = 0;

                    select sum(rank) abs, max(rank) max_rank 
                    from index 
                    where index.page_id = rec_page.page_id
                      and index.lemma_id in (select cast(unnest(string_to_array(lemma_id_array,',')) as integer))
                    into abs, max_rank; 
                    page_id = rec_page.page_id;
                    path = rec_page.path;
                    rel = abs / max_rank;
                    return next; 
                end loop;
            
            end
            $BODY$;""",nativeQuery = true)
    void createGetPagesFunction();
}
