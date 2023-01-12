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

    private final String ALL_SITES_FUNCTION = "CREATE OR REPLACE FUNCTION public.execsql(\n" +
            "\tincludelemma text)\n" +
            "    RETURNS TABLE(id integer, abs double precision, rel double precision, path text) \n" +
            "    LANGUAGE 'plpgsql'\n" +
            "    COST 100\n" +
            "    VOLATILE PARALLEL UNSAFE\n" +
            "    ROWS 1000\n" +
            "\n" +
            "AS $BODY$\n" +
            "declare sqlCommand text;\n" +
            "declare sqlText text;\n" +
            "declare new_lemma text;\n" +
            "declare commaCR text;\n" +
            "declare comma text;\n" +
            "declare queryName text;\n" +
            "declare i integer;\n" +
            "declare finalQuery text;\n" +
            "declare lemmas text;\n" +
            "declare stmt text;\n" +
            "declare result_row record;\n" +
            "begin\n" +
            "\tsqlCommand = ' as (select index.page_id, index.rank from index join lemma on (lemma.id = index.lemma_id) where lemma = ';\n" +
            "\tsqlText = '';\tcomma = ''; commaCR = ''; i = 0;\n" +
            "\tqueryName = 'query_';\n" +
            "\tfinalQuery = ''; lemmas = '';\n" +
            "  for new_lemma in (select unnest(string_to_array(includeLemma,',')))\n" +
            "  loop--##########################################################\n" +
            "  \t\tlemmas = lemmas || comma || '''' || new_lemma || '''';\n" +
            "  \n" +
            "  \t\tsqlText = sqlText || commaCR || queryName || i || sqlCommand ||  '''' || new_lemma || ''')';\n" +
            "\t\tcommaCR = ', ' || chr(10); comma = ', '; \n" +
            "\t\tif i > 0 then\n" +
            "\t\t\tfinalQuery = finalQuery || ' join ' || queryName || i || ' on ('|| queryName || 0 || '.page_id = ' || queryName || i || '.page_id)' || chr(10); \n" +
            "\t\tend if;\t\n" +
            "\t\ti = i + 1;\n" +
            "\t\t\n" +
            "  end loop;--#####################################################\n" +
            "  \n" +
            " \n" +
            "  stmt = 'with ' ||  sqlText || ', ' || chr(10) || 'findPages as (select ' || queryName || 0 || '.page_id, ' || queryName || 0 || '.rank from ' || queryName || 0 || chr(10) || \n" +
            "\t\t finalQuery || '), ' || chr(10) || \n" +
            "\t\t 'statisticQuery as (select index.page_id, sum(index.rank) abs, sum(index.rank)/max(index.rank) rel from findPages ' || chr(10) ||\n" +
            "\t\t 'join index on (index.page_id = findPages.page_id) ' || chr(10) ||\n" +
            "\t\t 'where index.lemma_id in (select id lemma_id from lemma where lemma in (' || lemmas || ')) ' || chr(10) || \n" +
            "\t\t 'group by index.page_id) ' || chr(10) ||\n" +
            "\t\t 'select page.id, abs, rel, path from page ' || chr(10) ||\n" +
            "\t\t 'join statisticQuery on (statisticQuery.page_id = page.id) ' || chr(10) ||\n" +
            "\t\t 'order by abs desc, rel asc';\n" +
            "\t\t \n" +
            "\t\t for result_row in execute(stmt)\n" +
            "\t\t loop\n" +
            "\t\t   return next;\n" +
            "\t\t end loop;\n" +
            "\t\t \n" +
            "end\n" +
            "$BODY$;\n";


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