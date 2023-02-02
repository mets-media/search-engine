package engine.repository;

import engine.dto.SiteAndContent;
import engine.entity.KeepLink;
import engine.entity.Lemma;
import engine.entity.PathTable;
import engine.mapper.KeepLinkMapper;
import engine.mapper.LemmaMapper;
import engine.mapper.PathTableMapper;
import engine.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

@Repository
public class ImplRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PathTableMapper pathTableMapper;
    @Autowired
    private LemmaMapper lemmaMapper;
    @Autowired
    private KeepLinkMapper keepLinkMapper;
    @PostConstruct
    private void createSQLContent() {
        SearchService.createSqlContent();
    }

    public List<PathTable> findPathTableItems(String sqlQuery, Pageable pageable) {
        sqlQuery += "\noffset :offset limit :limit";
        return jdbcTemplate.query(
                sqlQuery.replace(":offset", Long.toString(pageable.getOffset()))
                        .replace(":limit", Long.toString(pageable.getPageSize())), pathTableMapper);
    }

    public List<PathTable> findPathTableItems(String sqlQuery) {
        return jdbcTemplate.query(sqlQuery, pathTableMapper);
    }

    public List<Lemma> findLemmasInAllSites(String lemmas) {
        return jdbcTemplate.query(SearchService.getSQLByName("findLemmasInAllSites")
                .replace(":lemmaIn", lemmas), lemmaMapper);
    }
}