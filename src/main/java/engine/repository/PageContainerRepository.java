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
            "CREATE OR REPLACE FUNCTION inc_counter(counter_Name Text, return_Value integer)\n" +
                    "    RETURNS integer\n" +
                    "    LANGUAGE 'plpgsql'\n" +
                    "    COST 100\n" +
                    "    VOLATILE PARALLEL UNSAFE\n" +
                    "AS $BODY$\n" +
                    "begin\n" +
                    "\tperform nextval(counter_name);\n" +
                    "\treturn return_Value;\t\n" +
                    "end\n" +
                    "$BODY$;\n" +
                    "\n" +
            "CREATE OR REPLACE FUNCTION parsing_function()\n" +
            "    RETURNS trigger\n" +
            "    LANGUAGE 'plpgsql'\n" +
            "    COST 100\n" +
            "    VOLATILE NOT LEAKPROOF\n" +
            "AS $BODY$\n" +
            "declare lemmainfo text;\n" +
            "declare new_lemma text;\n" +
            "declare new_count integer;\n" +
            "declare new_rank real;\n" +
            "declare lemma_id integer;\n" +
            "declare page_id integer;\n" +
            "BEGIN\n" +
            "\twith page_insert as (\n" +
            "    insert into PAGE (Site_id, Path, Code, Content)\n" +
            "\tvalues (new.site_id, new.path, new.code, new.content)\n" +
            "\t--on conflict on constraint siteId_path_unique do nothing\n" +
            "\t--on conflict on constraint siteId_path_unique do update set code = new.code + inc_del_page_counter()\n" +
            "\ton conflict on constraint siteId_path_unique do update set code = new.code + inc_counter('page_del_count',0)\n" +
            "\treturning id)\n" +
            "    select id from page_insert into page_id; \n" +
            "\tfor lemmainfo in select unnest(string_to_array(new.lemmatization,';'))\n" +
            "\tloop\n" +
            "\t    if (length(lemmainfo) > 0) then \t    \n" +
            "\t\t\tnew_lemma = Split_Part(lemmaInfo,',',1);\n" +
            "\t\t\tnew_count = Cast(Split_Part(lemmaInfo,',',2) as integer);\n" +
            "\t\t\tnew_rank  = Cast(Split_Part(lemmaInfo,',',3) as real);\n" +
            "\t\t\twith lemma_upsert as (\n" +
            "\t\t\tinsert into LEMMA (Site_Id, Lemma,Frequency) \n" +
            "\t\t\t\tvalues (new.site_id, new_lemma, new_count) \n" +
            "\t\t\t\ton conflict on constraint siteId_lemma_unique \n" +
            "\t\t\t\t--do update set Frequency = LEMMA.Frequency + inc_del_lemma_counter()\n" +
            "\t\t\t\tdo update set Frequency = LEMMA.Frequency + inc_counter('lemma_del_count',1)\n" +
            "\t\t\t\treturning id)\n" +
            "\t\t\tselect id from lemma_upsert into lemma_id;\t\n" +
            "\n" +
            "\n\t\t\tif (page_id is not null) then\n" +
            "\t\t\t\t\tinsert into INDEX (page_id, lemma_id, rank) \n" +
            "\t\t\t\t\tvalues (page_id,lemma_id, new_rank);\n" +

            "\n\t\t\tend if;\n" +
            "\n" +
            "\t\tend if;\n" +
            "\tend loop;\n" +
            "\n" +
            "\tdelete from page_container where id = new.id;\n" +
            "\n" +
            "    RETURN NEW;\n" +
            "END;    \n" +
            "$BODY$;\n" +
            "\n" +
            "CREATE or replace TRIGGER page_trigger\n" +
            "    after INSERT\n" +
            "    ON public.page_container\n" +
            "    FOR EACH ROW\n" +
            "    EXECUTE FUNCTION parsing_function();",
            nativeQuery = true)
    void createTrigger();


    @Modifying
    @Transactional
    @Query(value="Select parse_page_container()",nativeQuery = true)
    Integer parsePageContainer();


}
