package engine.repository;

import engine.entity.Lemma;
import engine.entity.PathTable;
import engine.mapper.LemmaMapper;
import engine.mapper.PathTableMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PathTableRepository {

    private static final String SQL_REQUEST_RESULT_TABLE_FOR_SELECTED_SITE =
            "with lemma_query as (select unnest(string_to_array(:includeLemma,',')) lemma), " +

                    "lemma_id_query as (select id lemma_id	from lemma " +
                    "join lemma_query on (lemma.lemma = lemma_query.lemma) " +
                    "where site_id = :siteId), " +

                    "index_query as (select page_id, sum(rank) abs from index " +
                    "join lemma_id_query on (index.lemma_id = lemma_id_query.lemma_id) " +
                    "where page_id in (:includePageId)" +
                    "group by page_id), " +

                    "page_query as (select id page_id, abs, abs / (select max(abs) sum_abs from index_query) rel, path from page " +
                    "join index_query on (page.id = index_query.page_id)" +
                    "where page.id in (:includePageId)" +
                    ") " +

                    "select * from page_query " +
                    "order by rel desc, path";

    private static final String SQL_REQUEST_RESULT_TABLE_FOR_ALL_SITES =
            "with lemma_query as (select unnest(string_to_array(:includeLemma,',')) lemma), " +
                    "\n" +
                    "lemma_id_query as (select id lemma_id from lemma \n" +
                    "join lemma_query on (lemma.lemma = lemma_query.lemma) \n" +
                    "),\n" +
                    "\n" +
                    "index_query as (select page_id, sum(rank) abs from index \n" +
                    "join lemma_id_query on (index.lemma_id = lemma_id_query.lemma_id) \n" +
                    "\n" +
                    "group by page_id), \n" +
                    "\n" +
                    "page_query as (select id page_id, abs, abs / (select max(abs) max_abs from index_query) rel, path from page \n" +
                    "join index_query on (page.id = index_query.page_id)\n" +
                    "\n" +
                    ")\n" +
                    "\n" +
                    "select * from page_query \n" +
                    "order by rel desc, path";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PathTableMapper pathTableMapper;

    public List<PathTable> getResultTableForSelectedSite(Integer siteId, String includeLemma, String includePageId) {
        return jdbcTemplate.query(SQL_REQUEST_RESULT_TABLE_FOR_SELECTED_SITE
                .replace(":includeLemma", includeLemma)
                .replace(":siteId", siteId.toString())
                .replace(":includePageId", includePageId), pathTableMapper);
    }

    public List<PathTable> getResultTableForAllSites(String includeLemma, String includePageId) {
        return jdbcTemplate.query(SQL_REQUEST_RESULT_TABLE_FOR_ALL_SITES
                .replace(":includeLemma", includeLemma)
                .replace(":includePageId", includePageId), pathTableMapper);
    }

    @Autowired
    private LemmaMapper lemmaMapper;

    String FIND_LEMMA_IN_ALL_SITES = "select 0 id, sum(frequency) frequency, lemma, sum(rank) rank, 0 site_id \n" +
            "from lemma\n" +
            "where lemma in (:lemmaIn)\n" +
            "group by lemma\n" +
            "order by frequency";
    public List<Lemma> findLemmasInAllSites(String lemmas) {
        return jdbcTemplate.query(FIND_LEMMA_IN_ALL_SITES
                .replace(":lemmaIn", lemmas), lemmaMapper);

    }
}