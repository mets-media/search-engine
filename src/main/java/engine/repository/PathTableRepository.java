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

                    "lemma_id_query as (select id lemma_id	from lemma " +
                    "join lemma_query on (lemma.lemma = lemma_query.lemma) " +
                    "where site_id = :siteId), " +

                    "index_query as (select page_id, sum(rank) abs from index " +
                    "join lemma_id_query on (index.lemma_id = lemma_id_query.lemma_id) " +
                    "where page_id in (:includePageId)" +
                    "group by page_id), " +

                    "page_query as (select id page_id, abs, abs / (select max(abs) max_abs from index_query) rel, path from page " +
                    "join index_query on (page.id = index_query.page_id)" +
                    "where page.id in (:includePageId)" +
                    ") " +

                    "select * from page_query " +
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

    public List<PathTable> getResultByGetPage(String lemmas, String pageIntersection, Integer siteId) {
        return jdbcTemplate.query("select * from get_pages(:lemmas, :pageIntersection, :siteId)"
                .replace(":lemmas", lemmas)
                .replace(":pageIntersection", pageIntersection)
                .replace(":siteId", siteId.toString()), pathTableMapper);

    }

}