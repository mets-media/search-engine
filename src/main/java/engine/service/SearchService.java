package engine.service;

import engine.entity.IndexEntity;
import engine.entity.Lemma;
import engine.entity.PathTable;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@UtilityClass
public class SearchService {

    private static BeanAccess beanAccess;

    public static void setBeanAccess(BeanAccess beanAccess) {
        SearchService.beanAccess = beanAccess;
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
            return SearchService.getLemmaIdByLemmaNames(selectedLemmas.stream().map(Lemma::getLemma).toList());
        else //Леммы для выбранного сайта
            return selectedLemmas.stream().map(l -> l.getId().toString())
                .collect(Collectors.joining(","));
    };
    public static  List<PathTable> findIndexIntersection(Set<Lemma> lemmas,
                                                         HashMap<String, List<IndexEntity>> indexHashMap,
                                                         int siteId) {
        if (lemmas.size() == 0) return new ArrayList<>();

        List<IndexEntity> listIndex;

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
                    indexHashMap.get(lemma.getLemma()).stream().map(IndexEntity::getPageId).toList());
        }
        //Найти пересечение
        List<Integer> joinPageId = SearchService.retainAllPageId(lemmas, pageIdHashMap);

        //Вычитаем лишние IndexEntity
        List<IndexEntity> joinedIndexEntities = SearchService.removeByPageId(indexHashMap, joinPageId);

        return calcRelevanceForEachPage(joinedIndexEntities, lemmas, siteId);
    }

    public static Integer getSiteIdFromLemmas(int searchLemmaId, Set<Lemma> lemmas) {
        for (Lemma lemma : lemmas) {
            if (lemma.getId() == searchLemmaId) return lemma.getSiteId();
        }
        return -1;
    }

    private static List<PathTable> calcRelevanceForEachPage(List<IndexEntity> entities, Set<Lemma> lemmas, int selectedSiteId) {

        List<PathTable> result = new ArrayList<>();

        int pageId = 0;
        float abs = 0;
        float rel = 0;
        float max_rank = 0;

        for (IndexEntity indexEntity : entities) {

            int nextPage = indexEntity.getPageId();

            if ((nextPage != pageId) && (pageId != 0)) {
                rel = abs / max_rank;
                //get path
                int lemmaSiteId = getSiteIdFromLemmas(indexEntity.getLemmaId(), lemmas);

                //Добавляем в результат
                if ((lemmaSiteId == selectedSiteId) || (selectedSiteId == 0))
                    result.add(new PathTable(pageId, abs, rel, Integer.toString(pageId)));
                max_rank = 0;
                abs = 0;
                rel = 0;
            }
            pageId = indexEntity.getPageId();
            abs += indexEntity.getRank();
            if (max_rank < indexEntity.getRank()) max_rank = indexEntity.getRank();
        }
        if (abs != 0) { //Добавляем последнюю - если она есть
            rel = abs / max_rank;
            result.add(new PathTable(pageId, abs, rel, Integer.toString(pageId)));
        }
        result.sort(Comparator.comparing(PathTable::getAbsRelevance).reversed());
        return result;
    }

    public static String getLemmaIdByLemmaNames(List<String> lemmaNames) {
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

    public static List<PathTable> listMerge(List<PathTable> list1, List<PathTable> list2) {
        /**
         * Суммирование List1 List2
         */
        //-----------------------------------------------------------------------------------------------------
        BiFunction<PathTable,PathTable,PathTable> ADD_PATH_FUNCTION =
                (p1,p2) -> new PathTable(p1.getPageId(), p1.getAbsRelevance(), p1.getRelRelevance(), p2.getPath());
        //-----------------------------------------------------------------------------------------------------

        HashMap<Integer, PathTable> list1_Map = new HashMap<>();
        list1.stream().map(l -> new AbstractMap.SimpleEntry<Integer, PathTable>(l.getPageId(), l))
                .forEach(m -> list1_Map.put(m.getKey(), m.getValue()));

        HashMap<Integer, PathTable> list2_Map = new HashMap<>();
        list2.stream().map(l -> new AbstractMap.SimpleEntry<Integer, PathTable>(l.getPageId(), l))
                .forEach(m -> list2_Map.put(m.getKey(), m.getValue()));

        list2_Map.forEach((k,v) -> list1_Map.merge(k,v,ADD_PATH_FUNCTION));

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

    public List<IndexEntity> removeByPageId(HashMap<String, List<IndexEntity>> hashMapIndex,
                                             List<Integer> listPageId) {

        Set<IndexEntity> entityHashSet = new HashSet<>();

        for (String lemma : hashMapIndex.keySet()) {

            List<IndexEntity> indexEntities = hashMapIndex.get(lemma);

            indexEntities.forEach(indexEntity -> {
                if (listPageId.contains(indexEntity.getPageId()))
                    entityHashSet.add(indexEntity);
            });
        }

        return entityHashSet.stream().sorted(Comparator.comparing(IndexEntity::getPageId)).toList();
    }


    private List<IndexEntity> retainAllIndexes(Set<Lemma> lemmaSet, HashMap<String, List<IndexEntity>> hashMap) {
        List<Lemma> sortedLemma = lemmaSet
                .stream()
                .sorted(Comparator.comparing(Lemma::getFrequency)).toList();

        if (sortedLemma.size() == 0) return new ArrayList<>();

        String lowFrequencyLemma = sortedLemma.get(0).getLemma();

        List<IndexEntity> result = new ArrayList<>(hashMap.get(lowFrequencyLemma));
        for (int i = 1; i < sortedLemma.size(); i++) {
            lowFrequencyLemma = sortedLemma.get(i).getLemma();

            //result.retainAll(hashMap.get(lowFrequencyLemma));
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

}
