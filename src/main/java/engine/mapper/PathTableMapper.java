package engine.mapper;

import engine.entity.PathTable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class PathTableMapper implements RowMapper<PathTable> {
    @Override
    public PathTable mapRow(ResultSet rs, int rowNum) throws SQLException {
        PathTable pathTable = new PathTable();
        pathTable.setPath(rs.getString("path"));
        pathTable.setPageId(rs.getInt("page_id"));
        pathTable.setAbsRelevance(rs.getFloat("abs"));
        pathTable.setRelRelevance(rs.getFloat("rel"));
        return pathTable;
    }
}
