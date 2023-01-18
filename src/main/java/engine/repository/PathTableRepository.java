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
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PathTableMapper pathTableMapper;
    @Autowired
    private LemmaMapper lemmaMapper;

    private static final String SQL_RESULT_TABLE_FOR_SELECTED_SITE =
            "with lemma_id_query as (select cast(unnest(string_to_array(:lemma_id_array,',')) as integer) lemma_id), " +
                    "\n" +
                    "index_query as (select page_id, sum(rank) abs, max(rank) max_abs from index \n" +
                    "join lemma_id_query on (index.lemma_id = lemma_id_query.lemma_id) \n" +
                    "where index.page_id in (:page_id_array) \n" +
                    "group by index.page_id), \n" +
                    "\n" +
                    "page_query as (select id page_id, abs, abs / max_abs rel, path from page \n" +
                    "join index_query on (page.id = index_query.page_id) \n" +
                    "where page.id in (:page_id_array) \n" +
                    ")\n" +
                    "\n" +

                    "select * from page_query \n" +
                    "order by abs desc, rel desc";

    public List<PathTable> getResultTableForSelectedSite(String lemmaIdArray, String pageIdArray) {
        return jdbcTemplate.query(SQL_RESULT_TABLE_FOR_SELECTED_SITE
                .replace(":lemma_id_array", lemmaIdArray)
                .replace(":page_id_array", pageIdArray), pathTableMapper);
    }

    public List<PathTable> getResultTableForAllSites(String includeLemma) {
        return jdbcTemplate.query("select * from get_pages_generate_stmt(:includeLemma)"
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
        //return jdbcTemplate.query("select * from get_pages(:lemmas, :pageIntersection, :siteId)"
        return jdbcTemplate.query("select * from get_pages(:lemmas, :pageIntersection)"
                .replace(":lemmas", lemmas)
                .replace(":pageIntersection", pageIntersection)
                //.replace(":siteId", siteId.toString()), pathTableMapper);
                , pathTableMapper);
    }


    public List<PathTable> getPaths(String pages) {
        return jdbcTemplate.query("Select id page_id, path, cast(0 as float) abs, cast(0 as float) rel \n" +
                "from page \n" +
                "where id in (:pages)"
                        .replace(":pages",pages), pathTableMapper);
    }



}