package engine.mapper;

import engine.entity.KeepLink;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class KeepLinkMapper implements RowMapper<KeepLink> {
    @Override
    public KeepLink mapRow(ResultSet rs, int rowNum) throws SQLException {
        KeepLink keepLink = new KeepLink();
        keepLink.setId(rs.getInt("id"));
        keepLink.setCode(rs.getInt("code"));
        keepLink.setSiteId(rs.getInt("site_id"));
        keepLink.setStatus(rs.getInt("status"));
        keepLink.setPath(rs.getString("path"));
        return keepLink;
    }
}
