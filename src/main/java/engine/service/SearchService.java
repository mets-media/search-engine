package engine.service;

import engine.dto.SqlQueryDto;
import engine.entity.Index;
import engine.entity.Lemma;
import engine.entity.PathTable;
import engine.entity.Site;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;
import org.hibernate.loader.custom.sql.SQLQueryParser;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class SearchService {
    private static BeanAccess beanAccess;
    private static HashMap<String, SqlQueryDto> sqlContent;

    public static void setBeanAccess(BeanAccess beanAccess) {
        SearchService.beanAccess = beanAccess;
    }

    public static String getGeneratedSQL(Set<Lemma> selectedLemmas) {
        /** Генерация запроса - get_page_generate_stmt(:lemmaNames)
         * Для запроса нужны только имена лемм  **/
        String lemmaNames = selectedLemmas.stream().map(Lemma::getLemma)
                .collect(Collectors.joining(",", "'", "'"));

        return SearchService.getSQLByName("allSitesGenerateStmt")
                .replace(":lemmaNames", lemmaNames);
    }

    public static List<Integer> getCommonPages(Set<Lemma> selectedLemmas,
                                               int siteId, HashMap<String,
                                               List<Integer>> pageIdHashMap) {
        /** Заполняем HashMap<Лемма, List<Integer>> pageIdHashMap **/
        fillPageIdHashMap(selectedLemmas, siteId, pageIdHashMap);
        /** Retain all pageId - находим пересечение страниц для всех лемм **/
        return  retainAllPageId(selectedLemmas, pageIdHashMap);
    }

    public static String getLemmaIdString(Set<Lemma> selectedLemmas, int siteId) {
        /** Формируем строку с перечислением Lemma_Id **/
        if (siteId == 0) //Леммы для всех сайтов
            return SearchService.getLemmaIdArrayByLemmaNames(selectedLemmas.stream().map(Lemma::getLemma).toList());
        else //Леммы для выбранного сайта
            return selectedLemmas.stream().map(l -> l.getId().toString())
                .collect(Collectors.joining(","));
    };
    public static  List<PathTable> findIndexIntersection(Set<Lemma> lemmas,
                                                         HashMap<String, List<Index>> indexHashMap,
                                                         int siteId) {
        if (lemmas.size() == 0) return new ArrayList<>();

        List<Index> listIndex;

        for (Lemma lemma : lemmas) {
            String selectedLemma = lemma.getLemma();
            if (!indexHashMap.containsKey(selectedLemma)) {
                if (siteId == 0)
                    listIndex = beanAccess.getIndexRepository().getIndexByLemmaForAllSites(selectedLemma);
                else
                    listIndex = beanAccess.getIndexRepository().getIndexByLemmaForSiteId(selectedLemma, siteId);

                indexHashMap.put(selectedLemma, listIndex);
            }
        }
        //PageId
        HashMap<String, List<Integer>> pageIdHashMap = new HashMap<>();

        //Взять только выбранные леммы
        for (Lemma lemma : lemmas) {
            pageIdHashMap.put(lemma.getLemma(),
                    indexHashMap.get(lemma.getLemma()).stream().map(Index::getPageId).toList());
        }
        //Найти пересечение
        List<Integer> joinPageId = SearchService.retainAllPageId(lemmas, pageIdHashMap);

        //Вычитаем лишние IndexEntity
        List<Index> joinedIndexEntities = SearchService.removeByPageId(indexHashMap, joinPageId);

        return calcRelevanceForEachPage(joinedIndexEntities, lemmas, siteId);
    }

    public static Integer getSiteIdFromLemmas(int searchLemmaId, Set<Lemma> lemmas) {
        for (Lemma lemma : lemmas) {
            if (lemma.getId() == searchLemmaId) return lemma.getSiteId();
        }
        return -1;
    }

    private static List<PathTable> calcRelevanceForEachPage(List<Index> entities, Set<Lemma> lemmas, int selectedSiteId) {

        List<PathTable> result = new ArrayList<>();

        int pageId = 0;
        float abs = 0;
        float rel = 0;
        float max_rank = 0;

        for (Index index : entities) {

            int nextPage = index.getPageId();

            if ((nextPage != pageId) && (pageId != 0)) {
                rel = abs / max_rank;
                //get path
                int lemmaSiteId = getSiteIdFromLemmas(index.getLemmaId(), lemmas);

                //Добавляем в результат
                if ((lemmaSiteId == selectedSiteId) || (selectedSiteId == 0))
                    result.add(new PathTable(pageId, abs, rel, Integer.toString(pageId)));
                max_rank = 0;
                abs = 0;
                rel = 0;
            }
            pageId = index.getPageId();
            abs += index.getRank();
            if (max_rank < index.getRank()) max_rank = index.getRank();
        }
        if (abs != 0) { //Добавляем последнюю - если она есть
            rel = abs / max_rank;
            result.add(new PathTable(pageId, abs, rel, Integer.toString(pageId)));
        }
        result.sort(Comparator.comparing(PathTable::getAbsRelevance).reversed());
        return result;
    }

    public static String getLemmaIdArrayByLemmaNames(List<String> lemmaNames) {
        return beanAccess.getLemmaRepository()
                .findByLemmaIn(lemmaNames)
                .stream().map(l->l.getId().toString())
                .collect(Collectors.joining(","));
    }
    public static void fillPageIdHashMap(Set<Lemma> lemmas,
                                         int siteId,
                                         HashMap<String, List<Integer>> pageIdHashMap) {
        lemmas.forEach(lemma -> {
            String selectedLemma = lemma.getLemma();
            List<Integer> pageIdList;
            if (siteId == 0) //Для всех сайтов
                pageIdList = beanAccess.getPageRepository().getPageIdByLemma(selectedLemma);
            else //Для выбранного сайта
                pageIdList = beanAccess.getPageRepository().getPageIdBySiteIdAndLemma(selectedLemma, siteId);

            //Для каждой леммы - свой список страниц (pageId)
            if (!pageIdHashMap.containsKey(selectedLemma))
                pageIdHashMap.put(selectedLemma, pageIdList);
        });

    }

    public void refreshSitesInformation() {
        new Thread(() -> { //Обновление информации по сайтам
            for (Site site : beanAccess.getSiteRepository().getStatistic())
                beanAccess.getSiteRepository().save(site);
        }).start();
    }
    public static List<PathTable> listMergeEx(List<PathTable> list1, List<PathTable> list2) {
        /**  * Суммирование List1 List2  */
        Map<Integer, PathTable> list1_Map = list1.stream()
                .collect(Collectors.toMap(PathTable::getPageId, Function.identity()));

        Map<Integer, PathTable> list2_Map = list2.stream()
                .collect(Collectors.toMap(PathTable::getPageId, Function.identity()));

        Map<Integer, PathTable> resultMap = Stream.concat(list1_Map.entrySet().stream(),list2_Map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
        (l1,l2) -> new PathTable(l1.getPageId(),l1.getAbsRelevance(),l1.getRelRelevance(),l2.getPath())));

        return resultMap.values().stream().sorted(Comparator.comparing(PathTable::getAbsRelevance)).toList();

    }
    public static List<PathTable> listMerge(List<PathTable> list1, List<PathTable> list2) {
        /**  * Суммирование List1 List2  */

        Map<Integer, PathTable> list1_Map = list1.stream()
                .collect(Collectors.toMap(PathTable::getPageId, Function.identity()));

        Map<Integer, PathTable> list2_Map = list2.stream()
                .collect(Collectors.toMap(PathTable::getPageId, Function.identity()));

        list2_Map.forEach((k,v) -> list1_Map.merge(k,v,
                (p1,p2) -> new PathTable(p1.getPageId(), p1.getAbsRelevance(), p1.getRelRelevance(), p2.getPath())));

        return list1_Map.values().stream().sorted(Comparator.comparing(PathTable::getAbsRelevance).reversed()).toList();
    }

    public static List<Integer> retainAllPageId(Set<Lemma> selectedLemmas,
                                          HashMap<String, List<Integer>> hashMap) {

        if (selectedLemmas.size() == 0) return new ArrayList<>();

        List<Lemma> sortedLemma = selectedLemmas
                .stream()
                .sorted(Comparator.comparing(Lemma::getFrequency).reversed()).toList();

        String lemma = sortedLemma.get(0).getLemma();

        List<Integer> result = new ArrayList<>(hashMap.get(lemma));
        for (int i = 1; i < sortedLemma.size(); i++) {
            lemma = sortedLemma.get(i).getLemma();
            result.retainAll(hashMap.get(lemma));
        }
        //Collections.sort(result);
        return result;
    }

    public List<Index> removeByPageId(HashMap<String, List<Index>> hashMapIndex,
                                      List<Integer> listPageId) {

        Set<Index> entityHashSet = new HashSet<>();

        for (String lemma : hashMapIndex.keySet()) {

            List<Index> indexEntities = hashMapIndex.get(lemma);

            indexEntities.forEach(index -> {
                if (listPageId.contains(index.getPageId()))
                    entityHashSet.add(index);
            });
        }

        return entityHashSet.stream().sorted(Comparator.comparing(Index::getPageId)).toList();
    }


    private List<Index> retainAllIndexes(Set<Lemma> lemmaSet, HashMap<String, List<Index>> hashMap) {
        List<Lemma> sortedLemma = lemmaSet
                .stream()
                .sorted(Comparator.comparing(Lemma::getFrequency)).toList();

        if (sortedLemma.size() == 0) return new ArrayList<>();

        String lowFrequencyLemma = sortedLemma.get(0).getLemma();

        List<Index> result = new ArrayList<>(hashMap.get(lowFrequencyLemma));
        for (int i = 1; i < sortedLemma.size(); i++) {
            lowFrequencyLemma = sortedLemma.get(i).getLemma();

            result.retainAll(hashMap.get(lowFrequencyLemma));
        }
        return result;
    }

    private List<String> retainAllSelectedLemmas(Set<Lemma> lemmas, HashMap<String, List<String>> pagesHashMap) {

        List<Lemma> sortedLemma = lemmas
                .stream()
                .sorted(Comparator.comparing(Lemma::getFrequency)).toList();

        if (sortedLemma.size() == 0)
            return new ArrayList<>();

        String lemma = sortedLemma.get(0).getLemma();

        List<String> result = new ArrayList<>(pagesHashMap.get(lemma));
        for (int i = 1; i < sortedLemma.size(); i++) {
            lemma = sortedLemma.get(i).getLemma();
            result.retainAll(pagesHashMap.get(lemma));

        }
        Collections.sort(result);
        return result;
    }

    public static void createSqlContent() {
        sqlContent = new HashMap<>();

        String name = "oneSiteQuery";
        String sql = """
                    with lemma_id_query as (select cast(unnest(string_to_array(':lemmaIdArray',',')) as integer) lemma_id), 
                    index_query as (select page_id, sum(rank) abs, max(rank) max_abs from index 
                    join lemma_id_query on (index.lemma_id = lemma_id_query.lemma_id) 
                    where index.page_id in (:pageIdArray) 
                    group by index.page_id), 

                    page_query as (select id page_id, abs, abs / max_abs rel, path from page 
                    join index_query on (page.id = index_query.page_id) 
                    where page.id in (:pageIdArray) 
                    )

                    select * from page_query 
                    order by abs desc, rel desc
                """;

        sqlContent.put(name, new SqlQueryDto(name,sql));

        name = "allSitesGenerateStmt";
        sql = "select * from get_pages_generate_stmt(:lemmaNames)";
        sqlContent.put(name, new SqlQueryDto(name,sql));

        name = "findLemmasInAllSites";
        sql = """
                    select 0 id, sum(frequency) frequency, lemma, 0 site_id 
                    from lemma
                    where lemma in (:lemmaIn)
                    group by lemma
                    order by frequency""";
        sqlContent.put(name, new SqlQueryDto(name,sql));

        name = "getResult_INDEX_PAGE_LEMMA";
        sql = "select * from get_pages_index_page_lemma(':lemmaIdArray', ':pageIdArray', :siteId) " +
              "order by abs desc";
        sqlContent.put(name, new SqlQueryDto(name,sql));

        name = "getResult_GetPage_PAGE_INDEX";
        sql = "select * from get_pages_page_index(':lemmaIdArray', ':pageIdArray') " +
                "order by abs desc, rel";
        sqlContent.put(name, new SqlQueryDto(name,sql));

        name = "getPaths";
        sql = """
                Select id page_id, path, cast(0 as float) abs, cast(0 as float) rel
                from page
                where id in (:pageIdArray)""";
        sqlContent.put(name, new SqlQueryDto(name,sql));

        name = "getSiteInfoByPageId";
        sql = """
                select site.name, site.url from site
                join page on (page.site_id = site.id)
                where page.id = :pageId
                """;
        sqlContent.put(name, new SqlQueryDto(name,sql));

    }

    public static String getSQLByName(String nameSQLQuery) {
        return sqlContent.get(nameSQLQuery).sql();
    }
}
