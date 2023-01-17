package engine.service;

import engine.entity.IndexEntity;
import engine.entity.Lemma;
import lombok.experimental.UtilityClass;

import java.util.*;

@UtilityClass
public class SearchService {

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
