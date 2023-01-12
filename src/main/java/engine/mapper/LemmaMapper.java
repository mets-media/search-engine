package engine.mapper;


import engine.entity.Lemma;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class LemmaMapper implements RowMapper<Lemma> {

    @Override
    public Lemma mapRow(ResultSet rs, int rowNum) throws SQLException {
        Lemma lemma = new Lemma();

        lemma.setId(rs.getInt("id"));
        lemma.setLemma(rs.getString("lemma"));
        lemma.setSiteId(rs.getInt("site_id"));
        lemma.setFrequency(rs.getInt("frequency"));
        //lemma.setRank(rs.getFloat("rank"));

        return lemma;
    }
}
