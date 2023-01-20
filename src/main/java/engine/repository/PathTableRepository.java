package engine.repository;

import engine.entity.Lemma;
import engine.entity.PathTable;
import engine.mapper.LemmaMapper;
import engine.mapper.PathTableMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
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
            """
            with lemma_id_query as (select cast(unnest(string_to_array(':lemma_id_array',',')) as integer) lemma_id),\s
                    index_query as (select page_id, sum(rank) abs, max(rank) max_abs from index\s
                    join lemma_id_query on (index.lemma_id = lemma_id_query.lemma_id)\s
                    where index.page_id in (:page_id_array)\s
                    group by index.page_id),\s

                    page_query as (select id page_id, abs, abs / max_abs rel, path from page\s
                    join index_query on (page.id = index_query.page_id)\s
                    where page.id in (:page_id_array)\s
                    )

                    select * from page_query\s
                    order by abs desc, rel desc""";

    public List<PathTable> getResult_Query(String lemmaIdArray, String pageIdArray) {
        return jdbcTemplate.query(SQL_RESULT_TABLE_FOR_SELECTED_SITE
                .replace(":lemma_id_array", lemmaIdArray)
                .replace(":page_id_array", pageIdArray), pathTableMapper);
    }

    public List<PathTable> getResult_Generate_STMT(String includeLemma) {
        return jdbcTemplate.query("select * from get_pages_generate_stmt(:includeLemma)"
                .replace(":includeLemma", includeLemma), pathTableMapper);
    }

    String FIND_LEMMA_IN_ALL_SITES =
            """
                    select 0 id, sum(frequency) frequency, lemma, 0 site_id\s
                    from lemma
                    where lemma in (:lemmaIn)
                    group by lemma
                    order by frequency""";

    public List<Lemma> findLemmasInAllSites(String lemmas) {
        return jdbcTemplate.query(FIND_LEMMA_IN_ALL_SITES
                .replace(":lemmaIn", lemmas), lemmaMapper);
    }

    public List<PathTable> getResult_INDEX_PAGE_LEMMA(String lemmaIdArray, String pageIdArray, Integer siteId) {
        return jdbcTemplate.query(("select * from get_pages_index_page_lemma(':lemmaIdArray', ':pageIdArray', :siteId)" +
                " order by abs desc")
                .replace(":lemmaIdArray", lemmaIdArray)
                .replace(":pageIdArray", pageIdArray)
                .replace(":siteId", siteId.toString()), pathTableMapper);
    }

    public List<PathTable> getResult_GetPage_PAGE_INDEX(String lemmaIdArray, String pageIdArray) {
        return jdbcTemplate.query(("select * from get_pages_page_index(':lemmaIdArray', ':pageIdArray') " +
                        "order by abs desc, rel")
                        .replace(":lemmaIdArray", lemmaIdArray)
                        .replace(":pageIdArray", pageIdArray)
                , pathTableMapper);
    }

    public List<PathTable> getPaths(String pages) {
        return jdbcTemplate.query("Select id page_id, path, cast(0 as float) abs, cast(0 as float) rel \n" +
                "from page \n" +
                "where id in (:pages)"
                        .replace(":pages", pages), pathTableMapper);
    }

    private static final String SQL =
            """
            with lemma_id_query as (select cast(unnest(string_to_array(':lemma_id_array',',')) as integer) lemma_id),\s
                 index_query as (select page_id, sum(rank) abs, max(rank) max_abs from index\s
                 join lemma_id_query on (index.lemma_id = lemma_id_query.lemma_id)\s
                 where index.page_id in (:page_id_array)\s
                 group by index.page_id),\s

                 page_query as (select id page_id, abs, abs / max_abs rel, path from page\s
                 join index_query on (page.id = index_query.page_id)\s
                 where page.id in (:page_id_array)\s
                 )

                 select * from page_query\s
                 order by abs desc, rel desc""";
    public List<PathTable> getResult_Pageable(String lemmaIdArray, String pageIdArray, Pageable pageable) {
        return jdbcTemplate.query(SQL.replace(":lemma_id_array", lemmaIdArray)
                .replace(":page_id_array", pageIdArray), pathTableMapper);
    }

    public List<PathTable> getResult_FromAllSites(String lemmaNames, Pageable pageable) {

        String sql = "select * from get_pages_generate_stmt(:lemmaNames) offset :offset limit :limit"
                .replace(":lemmaNames", lemmaNames)
                .replace(":offset", Long.toString(pageable.getOffset()) )
                .replace(":limit", Long.toString(pageable.getPageSize()) );
        return jdbcTemplate.query(sql, pathTableMapper);
    }


}