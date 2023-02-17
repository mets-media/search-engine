package engine.repository;

import engine.entity.PageContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PageContainerRepository extends JpaRepository<PageContainer, Integer>{
    @Modifying
    @Transactional
    @Query(value = """
            CREATE OR REPLACE FUNCTION parse_page_container(
            )
                RETURNS INTEGER
                LANGUAGE 'plpgsql'
                COST 100
                VOLATILE PARALLEL UNSAFE
            AS $BODY$
            declare container record;
            declare lemmainfo text;
            declare new_lemma text;
            declare new_count integer;
            declare new_rank real;
            declare lemma_id integer;
            declare page_id integer;
            declare page_count integer;
            begin

            for container in (Select * from page_container)
            loop
                page_count = page_count + 1;
                with page_insert as (
                insert into PAGE (Site_id, Path, Code, Content)
            \tvalues (container.site_id, container.path, container.code, container.content)
            \t--on conflict on constraint siteId_path_unique do nothing
            \treturning id)
                select id from page_insert into page_id;
            \t
            \tfor lemmainfo in select unnest(string_to_array(container.lemmatization,';'))
            \tloop
            \t    if (length(lemmainfo) > 0) then 
            \t \t\tnew_lemma = Split_Part(lemmaInfo,',',1);
            \t  \t\tnew_count = Cast(Split_Part(lemmaInfo,',',2) as Integer);
            \t\t\tnew_rank  = Cast(Split_Part(lemmaInfo,',',3) as real);

            \t\t\twith lemma_upsert as (
            \t\t\tinsert into LEMMA (Site_Id, Lemma,Frequency, Rank)\s
            \t\t\t\tvalues (container.site_id, new_lemma, new_count, new_rank)\s
            \t\t\t\ton conflict on constraint siteId_lemma_unique\s
            \t\t\t\tdo update set Frequency = LEMMA.Frequency + 1
            \t\t\t\treturning id)
            \t\t\tselect id from lemma_upsert into lemma_id;\t

            \t\t
            if (page_id notnull) then
            \t\t\tinsert into INDEX (page_id, lemma_id, rank)\s
            \t\t\tvalues (page_id,lemma_id, new_rank);
            \t\t
            end if;
            \t\tend if;\t
            \tend loop;
            \tdelete from page_container where id = container.id;
            end loop;
            return page_count;end;\s
            $BODY$;""",
            nativeQuery = true)
    void createFunction();


    @Modifying
    @Transactional
    @Query(value =
            """
                    CREATE OR REPLACE FUNCTION inc_counter(counter_Name Text, return_Value integer)
                        RETURNS integer
                        LANGUAGE 'plpgsql'
                        COST 100
                        VOLATILE PARALLEL UNSAFE
                    AS $BODY$
                    begin
                      perform nextval(counter_name);
                      return return_Value; 
                    end
                    $BODY$;

                    CREATE OR REPLACE FUNCTION parsing_function()
                        RETURNS trigger
                        LANGUAGE 'plpgsql'
                        COST 100
                        VOLATILE NOT LEAKPROOF
                    AS $BODY$
                    declare lemmainfo text;
                    declare new_lemma text;
                    declare new_count integer;
                    declare new_rank real;
                    declare lemma_id integer;
                    declare page_id integer;
                    BEGIN
                      with page_insert as (
                        insert into PAGE (Site_id, Path, Code, Content)
                      values (new.site_id, new.path, new.code, new.content)
                      --on conflict on constraint siteId_path_unique do nothing
                      --on conflict on constraint siteId_path_unique do update set code = new.code + inc_del_page_counter()
                      on conflict on constraint siteId_path_unique do update set code = new.code + inc_counter('page_del_count',0)
                      returning id)
                        select id from page_insert into page_id; 
                      for lemmainfo in select unnest(string_to_array(new.lemmatization,';'))
                      loop
                          if (length(lemmainfo) > 0) then    
                                new_lemma = Split_Part(lemmaInfo,',',1);
                                new_count = Cast(Split_Part(lemmaInfo,',',2) as integer);
                                new_rank  = Cast(Split_Part(lemmaInfo,',',3) as real);
                                with lemma_upsert as (
                                insert into LEMMA (Site_Id, Lemma,Frequency) 
                                    values (new.site_id, new_lemma, new_count)
                                    on conflict on constraint siteId_lemma_unique
                                    --do update set Frequency = LEMMA.Frequency + inc_del_lemma_counter()
                                    do update set Frequency = LEMMA.Frequency + inc_counter('lemma_del_count',1)
                                    returning id)
                                select id from lemma_upsert into lemma_id; 


                                if (page_id is not null) then
                                    insert into INDEX (page_id, lemma_id, rank) 
                                    values (page_id,lemma_id, new_rank);

                                end if;

                            end if;
                      end loop;

                      delete from page_container where id = new.id;

                        RETURN NEW;
                    END;   
                    $BODY$;

                    CREATE or replace TRIGGER page_trigger
                        after INSERT
                        ON public.page_container
                        FOR EACH ROW     EXECUTE FUNCTION parsing_function();""",
            nativeQuery = true)
    void createTrigger();
    }
