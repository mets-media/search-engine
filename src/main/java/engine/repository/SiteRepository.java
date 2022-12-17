package engine.repository;

import engine.entity.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> getSiteByUrl(String url);

    @Query(value = "Select * from Site " +
            "where id in (select distinct Site_Id from page) " +
            "order by url", nativeQuery = true)
    Page<Site> getSitesFromPageTable(Pageable pageable);

    @Modifying
    @Transactional
    @Query(value="CREATE OR REPLACE FUNCTION delete_site_function()\n" +
            "    RETURNS trigger\n" +
            "    LANGUAGE 'plpgsql'\n" +
            "    COST 100\n" +
            "    VOLATILE NOT LEAKPROOF\n" +
            "AS $BODY$\n" +
            "begin\n" +
            "  delete from keep_link where site_id = old.id;\n" +
            "  delete from lemma where site_id = old.id;\n" +
            "  delete from index where page_id in (select id from page where site_id = old.id);\n" +
            "  delete from page where site_id = old.id;\n" +
            "  return null;" +
            "end\n" +
            "$BODY$;\n" +

            "CREATE OR REPLACE TRIGGER delete_site_trigger\n" +
            "    BEFORE DELETE\n" +
            "    ON site\n" +
            "    FOR EACH ROW\n" +
            "    EXECUTE FUNCTION delete_site_function();",nativeQuery = true)
    void createTrigger();
}
