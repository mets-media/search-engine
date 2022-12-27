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
    @Query(value = "CREATE OR REPLACE FUNCTION parse_page_container(\n" +
            "\t)\n" +
            "    RETURNS INTEGER\n" +
            "    LANGUAGE 'plpgsql'\n" +
            "    COST 100\n" +
            "    VOLATILE PARALLEL UNSAFE\n" +
            "AS $BODY$\n" +
            "declare container record;\n" +
            "declare lemmainfo text;\n" +
            "declare new_lemma text;\n" +
            "declare new_count integer;\n" +
            "declare new_rank real;\n" +
            "declare lemma_id integer;\n" +
            "declare page_id integer;\n" +
            "declare page_count integer;" +
            "\n" +
            "begin\n" +
            "\n" +
            "for container in (Select * from page_container) \n" +
            "loop\n" +
            "    page_count = page_count + 1;\n" +
            "    with page_insert as (\n" +
            "    insert into PAGE (Site_id, Path, Code, Content)\n" +
            "\tvalues (container.site_id, container.path, container.code, container.content)\n" +
            "\t--on conflict on constraint siteId_path_unique do nothing\n" +
            "\treturning id)\n" +
            "    select id from page_insert into page_id; \n" +
            "\t\n" +
            "\tfor lemmainfo in select unnest(string_to_array(container.lemmatization,';'))\n" +
            "\tloop\n" +
            "\t    if (length(lemmainfo) > 0) then \t    \n" +
            "\t \t\tnew_lemma = Split_Part(lemmaInfo,',',1);\n" +
            "\t  \t\tnew_count = Cast(Split_Part(lemmaInfo,',',2) as Integer);\n" +
            "\t\t\tnew_rank  = Cast(Split_Part(lemmaInfo,',',3) as real);\n" +
            "\n" +
            "\t\t\twith lemma_upsert as (\n" +
            "\t\t\tinsert into LEMMA (Site_Id, Lemma,Frequency, Rank) \n" +
            "\t\t\t\tvalues (container.site_id, new_lemma, new_count, new_rank) \n" +
            "\t\t\t\ton conflict on constraint siteId_lemma_unique \n" +
            "\t\t\t\tdo update set Frequency = LEMMA.Frequency + 1\n" +
            "\t\t\t\treturning id)\n" +
            "\t\t\tselect id from lemma_upsert into lemma_id;\t\n" +

            "\n" +
            "\t\t\nif (page_id != null) then\n" +
            "\t\t\tinsert into INDEX (page_id, lemma_id, rank) \n" +
            "\t\t\tvalues (page_id,lemma_id, new_rank);\n" +
            "\t\t\nend if;\n" +

            "\t\tend if;\t\n" +
            "\tend loop;\n" +
            "\tdelete from page_container where id = container.id;\n" +
            "end loop;\n" +
            "return page_count;" +
            "end; \n" +
            "$BODY$;",
            nativeQuery = true)
    void createFunction();


    @Modifying
    @Transactional
    @Query(value = "CREATE OR REPLACE FUNCTION new_page_function()\n" +
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
            "\ton conflict on constraint siteId_path_unique do nothing\n" +
            "\treturning id)\n" +
            "    select id from page_insert into page_id; \n" +
            "\tfor lemmainfo in select unnest(string_to_array(new.lemmatization,';'))\n" +
            "\tloop\n" +
            "\t    if (length(lemmainfo) > 0) then \t    \n" +
            "\t \t\tnew_lemma = Split_Part(lemmaInfo,',',1);\n" +
            "\t  \t\tnew_count = Cast(Split_Part(lemmaInfo,',',2) as Integer);\n" +
            "\t\t\tnew_rank  = Cast(Split_Part(lemmaInfo,',',3) as real);\n" +
            "\t\t\twith lemma_upsert as (\n" +
            "\t\t\tinsert into LEMMA (Site_Id, Lemma,Frequency, Rank) \n" +
            "\t\t\t\tvalues (new.site_id, new_lemma, new_count, new_rank) \n" +
            "\t\t\t\ton conflict on constraint siteId_lemma_unique \n" +
            "\t\t\t\tdo update set Frequency = LEMMA.Frequency + 1\n" +
            "\t\t\t\treturning id)\n" +
            "\t\t\tselect id from lemma_upsert into lemma_id;\t\n" +
            "\n" +
            "\n\tif (page_id != null) then\n" +
            "\t\t\tinsert into INDEX (page_id, lemma_id, rank) \n" +
            "\t\t\tvalues (page_id,lemma_id, new_rank);\n" +
            "\n\tend if;\n" +
            "\n" +
            "\t\tend if;\n" +
            "\tend loop;\n" +
            "\n" +
            "\tdelete from page_container where id = new.id;\n" +
            "\n" +
            "    RETURN NULL;\n" +
            "END;    \n" +
            "$BODY$;\n" +
            "\n" +
            "CREATE or replace TRIGGER page_trigger\n" +
            "    AFTER INSERT\n" +
            "    ON public.page_container\n" +
            "    FOR EACH ROW\n" +
            "    EXECUTE FUNCTION new_page_function();",
            nativeQuery = true)
    void createTrigger();


    @Modifying
    @Transactional
    @Query(value="Select parse_page_container()",nativeQuery = true)
    Integer parsePageContainer();


    Integer countBySiteId(Integer siteId);

}
