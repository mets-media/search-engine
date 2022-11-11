package engine.repository;

import engine.entity.Page;
import net.bytebuddy.implementation.bind.annotation.Super;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class PageBatchPreparedStatementSetter implements BatchPreparedStatementSetter {

    private ConcurrentHashMap<String, Page> pages;


    public PageBatchPreparedStatementSetter(ConcurrentHashMap<String, Page> pages) {
        super();
        this.pages = pages;
    }

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        Integer countPages = pages.size();

        while (i < countPages) {
            Page page = pages.values().iterator().next();
            ps.setInt(1, page.getSiteId());
            ps.setInt(2, page.getCode());
            ps.setString(3, page.getPath());
            ps.setString(4, page.getContent());
        }
    }

    @Override
    public int getBatchSize() {
        return pages.size();
    }
}
