package engine.repository;

import engine.entity.Lemma;
import engine.entity.PathTable;
import engine.mapper.LemmaMapper;
import engine.mapper.PathTableMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PathTableRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PathTableMapper pathTableMapper;
    @Autowired
    private LemmaMapper lemmaMapper;
    private static final String SQL_REQUEST_RESULT_TABLE_FOR_SELECTED_SITE =
            "with lemma_query as (select unnest(string_to_array(:includeLemma,',')) lemma), " +

                    "lemma_id_query as (select id lemma_id	from lemma \n" +
                    "join lemma_query on (lemma.lemma = lemma_query.lemma) \n" +
                    "where site_id = :siteId), \n" +
                    "\n" +

                    "index_query as (select page_id, sum(rank) abs from index \n" +
                    "join lemma_id_query on (index.lemma_id = lemma_id_query.lemma_id) \n" +
                    "where page_id in (:includePageId) \n" +
                    "group by page_id), \n" +
                    "\n" +

                    "page_query as (select id page_id, abs, abs / (select max(abs) max_abs from index_query) rel, path from page \n" +
                    "join index_query on (page.id = index_query.page_id) \n" +
                    "where page.id in (:includePageId) \n" +
                    ") \n" +
                    "\n" +

                    "select * from page_query \n" +
                    "order by rel desc, path";

    public List<PathTable> getResultTableForSelectedSite(Integer siteId, String includeLemma, String includePageId) {
        return jdbcTemplate.query(SQL_REQUEST_RESULT_TABLE_FOR_SELECTED_SITE
                .replace(":includeLemma", includeLemma)
                .replace(":siteId", siteId.toString())
                .replace(":includePageId", includePageId), pathTableMapper);
    }

    public List<PathTable> getResultTableForAllSites(String includeLemma) {
        return jdbcTemplate.query("select * from search_lemma_all_sites(:includeLemma)"
                .replace(":includeLemma", includeLemma), pathTableMapper);
    }

    String FIND_LEMMA_IN_ALL_SITES =
            "select 0 id, sum(frequency) frequency, lemma, 0 site_id \n" +
                    "from lemma\n" +
                    "where lemma in (:lemmaIn)\n" +
                    "group by lemma\n" +
                    "order by frequency";

    public List<Lemma> findLemmasInAllSites(String lemmas) {
        return jdbcTemplate.query(FIND_LEMMA_IN_ALL_SITES
                .replace(":lemmaIn", lemmas), lemmaMapper);
    }

    public List<PathTable> getResultByLemmasAndSiteId(String lemmas, String pageIntersection, Integer siteId) {
        return jdbcTemplate.query("select * from get_by_lemma_and_site(:lemmas, :pageIntersection, :siteId)"
                .replace(":lemmas", lemmas)
                .replace(":pageIntersection", pageIntersection)
                .replace(":siteId", siteId.toString()), pathTableMapper);
    }

    public List<PathTable> getResult_Function_GetPage(String lemmas, String pageIntersection, Integer siteId) {
        return jdbcTemplate.query("select * from get_pages(:lemmas, :pageIntersection, :siteId)"
                .replace(":lemmas", lemmas)
                .replace(":pageIntersection", pageIntersection)
                .replace(":siteId", siteId.toString()), pathTableMapper);
    }


    public List<PathTable> getPaths(String pages) {
        return jdbcTemplate.query("Select id page_id, path, cast(0 as float) abs, cast(0 as float) rel \n" +
                "from page \n" +
                "where id in (:pages)"
                        .replace(":pages",pages), pathTableMapper);
    }



}