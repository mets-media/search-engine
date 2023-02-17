package engine.repository;

import engine.dto.IdTextDto;
import engine.entity.KeepLink;
import engine.enums.LinkStatus;
import engine.service.BeanAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public interface KeepLinkRepository extends JpaRepository<KeepLink, Integer> {
    @Query(value="Select path from Keep_Link Where Site_Id = :siteId and Status = :status", nativeQuery = true)
    List<String> getPathsBySiteId(@Param("siteId") Integer siteId, @Param("status") Integer status);

    @Transactional
    @Modifying
    void deleteBySiteId(Integer siteId);

    @Transactional
    @Modifying
    void deleteByPath(String path);

    List<KeepLink> findBySiteIdAndCodeAndStatus(int siteId, int code, int status);

    List<KeepLink> findBySiteIdAndStatus(int siteId, int status);

    @Query(value = """
            select distinct new engine.dto.IdTextDto
            (
            code,
            case
              when code = -1 then 'Неизвестная ошибка'  
              when code = -2 then 'Timout при загрузке страницы' 
              else concat('код ошибки ', code) 
            end
            )
            from KeepLink
            where status = :status 
              and site_id = :siteId""")
    List<IdTextDto> getDistinctErrors(@Param("siteId") Integer siteId, @Param("status")Integer status);

    @Transactional
    @Modifying
    @Query(value = """
            drop table keep_link;
            CREATE TABLE IF NOT EXISTS public.keep_link
            (id  serial not null,
            code integer,
            path text NOT NULL,
            site_id integer NOT NULL,
            status integer,
            CONSTRAINT keep_link_pkey PRIMARY KEY (id)
            )""", nativeQuery = true)
    void reCreateTable();


}
