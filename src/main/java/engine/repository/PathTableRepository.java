package engine.repository;

import engine.entity.PathTable;
import engine.entity.PathTableMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PathTableRepository {

    private static final String SQL_REQUEST_RESULT_TABLE =
            "with lemma_query as (select unnest(string_to_array(:includeLemma,';')) lemma), " +

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
            "order by rel desc";

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PathTableMapper pathTableMapper;

    public List<PathTable> getResultTable(Integer siteId, String includeLemma, String includePageId) {
        return jdbcTemplate.query(SQL_REQUEST_RESULT_TABLE
                .replace(":includeLemma", includeLemma)
                .replace(":siteId", siteId.toString())
                .replace(":includePageId", includePageId),pathTableMapper);
    }
}
