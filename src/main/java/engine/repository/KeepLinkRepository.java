package engine.repository;

import engine.dto.IdTextDto;
import engine.entity.KeepLink;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    List<KeepLink> findBySiteId(Integer siteId);
    List<KeepLink> findBySiteId(Integer siteId, Pageable pageable);

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

    @Query(value = """
            Select distinct
             case
               when code is null then 'null'
               else cast(code as varchar(3))
             end code
            from Keep_Link
            where site_id = :siteId
              and status = :status
            order by code""", nativeQuery = true)
    List<String> getDistinctCodeBySiteId(@Param("siteId") int siteId, @Param("status") int status);

    @Transactional
    @Modifying
    @Query(value = "drop table keep_link;\n" +
            "CREATE TABLE IF NOT EXISTS public.keep_link\n" +
            "(id  serial not null, \n" +
            "code integer,\n" +
            "path text NOT NULL,\n" +
            "site_id integer NOT NULL,\n" +
            "status integer,\n" +
            "CONSTRAINT keep_link_pkey PRIMARY KEY (id)\n" +
            ")", nativeQuery = true)
    void reCreateTable();


}
