package engine.repository;

import engine.entity.PathTable;
import engine.entity.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    Optional<Site> getSiteByUrl(String url);

    @Query(value = "Select * from Site " +
            "where id in (select distinct Site_Id from page) " +
            "order by url", nativeQuery = true)
    Page<Site> getSitesFromPageTable(Pageable pageable);

    @Query(value =
            "Select * from Site \n" +
            "where id in (select distinct Site_Id from page) \n" +
            "order by id", nativeQuery = true)
    List<Site> getSitesFromPageTable();

    @Modifying
    @Transactional
    @Query(value="Update Site set page_count = :pageCount Where id = :siteId",nativeQuery = true)
    void setPageCountBySiteId(@Param("siteId") Integer siteId, @Param("pageCount") Integer pageCount);


    @Modifying
    @Transactional
    @Query(value = "Select delete_site_information(:siteId)", nativeQuery = true)
    Integer deleteSiteInformation(@Param("siteId") Integer siteId);

    @Query(value =
            "with pages as (select site_id, count(*) page_count from page group by site_id),\n" +
            "     lemmas as (select site_id, count(*) lemma_count from lemma group by site_id ),\n" +
            "    indexes as (select page.site_id, count(*) index_count from index\n" +
            "                 join page on (index.page_id = page.id)\n" +
            "                 group by page.site_id)\n" +
            "    select id, name, url, pages.site_id, pages.page_count, lemmas.lemma_count, indexes.index_count, status, status_time, last_error from pages\n" +
            "    join lemmas on (pages.site_id = lemmas.site_id)\n" +
            "    join indexes on (pages.site_id = indexes.site_id)\n" +
            "    join site on (pages.site_id = site.id)", nativeQuery = true)
    List<Site> getStatistic();


    @Query(value =
            "select 0 id, '*' name, 'Все сайты' url,\n" +
                    "(select last_value from page_id_seq)  - (select last_value - 1 from page_del_count) page_count,\n" +
                    "(select last_value from lemma_id_seq) - (select last_value - 1 from lemma_del_count) lemma_count,\n" +
                    "(select last_value from index_id_seq) - (select last_value - 1 from index_del_count) index_count,\n" +
                    "null status, now() status_time, '' last_error \n" +
                    "from one_record_table;\n",
            nativeQuery = true)
    Optional<Site> getAllSiteInfo();


    @Query(value = "select * from get_by_lemma_and_site(:lemmas, :siteId)",nativeQuery = true)
    List<PathTable> getResultByLemmasAndSiteId(@Param("lemmas") String lemmas, @Param("siteId") Integer siteId);

/*
DO $$declare
    declare record Record;
    declare page_count_total integer;
    declare lemma_count_total integer;
    declare index_count_total integer;

    declare page_ integer;
    declare lemma_ integer;
    declare index_ integer;

begin
	page_count_total = 0;
	lemma_count_total = 0;
	index_count_total = 0;

	for record in
	(with pages as (select site_id, count(*) page_count from page group by site_id),
		lemmas as (select site_id, count(*) lemma_count from lemma group by site_id ),
    	indexes as (select page.site_id, count(*) index_count from index
    	join page on (index.page_id = page.id)
    	group by page.site_id)
    	select id, name, url, pages.site_id, pages.page_count, lemmas.lemma_count, indexes.index_count, status, status_time, last_error from pages
    	join lemmas on (pages.site_id = lemmas.site_id)
    	join indexes on (pages.site_id = indexes.site_id)
    	join site on (pages.site_id = site.id)
	)
    loop
    	update site
    	set page_count = record.page_count,
            lemma_count= record.lemma_count,
            index_count = record.index_count
    	where site.id = record.id;
		page_count_total = page_count_total + record.page_count;
		lemma_count_total = lemma_count_total + record.lemma_count;
		index_count_total = index_count_total + record.index_count;
	end loop;

	select last_value - page_count_total from page_id_seq into page_;
	select last_value - lemma_count_total from lemma_id_seq into lemma_;
	select last_value - index_count_total from index_id_seq into index_;

	perform setval('page_del_count', page_ + 1);
	perform setval('lemma_del_count', lemma_ + 1);
	perform setval('index_del_count', index_ + 1);


end;$$
*/
}
