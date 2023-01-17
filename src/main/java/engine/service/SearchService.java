package engine.service;

import engine.entity.IndexEntity;
import engine.entity.Lemma;
import engine.entity.PathTable;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.function.BiFunction;

@UtilityClass
public class SearchService {

    public static List<PathTable> listMerge(List<PathTable> list1, List<PathTable> list2) {

        BiFunction<PathTable,PathTable,PathTable> ADD_PATH_FUNCTION =
                (p1,p2) -> new PathTable(p1.getPageId(),p1.getAbsRelevance(),p1.getRelRelevance(),p2.getPath());

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

        List<Lemma> sortedLemma = selectedLemmas
                .stream()
                .sorted(Comparator.comparing(Lemma::getFrequency)).toList();

        if (sortedLemma.size() == 0)
            return new ArrayList<>();

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
