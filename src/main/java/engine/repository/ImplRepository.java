package engine.repository;

import engine.entity.Lemma;
import engine.entity.PathTable;
import engine.enums.LinkStatus;
import engine.mapper.KeepLinkMapper;
import engine.mapper.LemmaMapper;
import engine.mapper.PathTableMapper;
import engine.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeToKeepLink(Integer siteId, LinkStatus status, List<String> links) {
        String sql = "Insert into Keep_Link (Site_Id, Status, Path) values (?,?,?)";
        int[] result = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String link = links.get(i);
                ps.setInt(1, siteId);
                ps.setInt(2, status.ordinal());
                ps.setString(3, link);
            }

            @Override
            public int getBatchSize() {
                return links.size();
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeToKeepLinkHM(Integer siteId,
                                         List<String> links,
                                         List<Integer> codes,
                                         LinkStatus status) {
        String sql = "Insert into Keep_Link (Site_Id, Path, Code, Status) values (?,?,?,?)";
        int[] result = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, siteId);
                ps.setString(2, links.get(i));
                ps.setInt(3, codes.get(i));
                ps.setInt(4, status.ordinal());
            }
            @Override
            public int getBatchSize() {
                return links.size();
            }
        });
    }

}