package engine.service;

import engine.entity.Field;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.function.BiFunction;

import static engine.service.HtmlParsing.getRussianWords;

public class Lemmatization {
    private final LuceneMorphology luceneMorph;
    private final List<String> excludeList;
    private final List<Field> cssSelectors;

    public Lemmatization(List<String> excludeList, List<Field> cssSelectors) {
        this.excludeList = excludeList;
        this.cssSelectors = cssSelectors;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean hasProperty(String wordBaseForm) {
        for (String excludeProp : excludeList) {
            String[] form = wordBaseForm.split(" ");
            if (form[1].equals(excludeProp))
                return true;
        }
        return false;
    }

    private Boolean includeForm(String lemma) {
        List<String> lemmaForms = luceneMorph.getMorphInfo(lemma);
        return !lemmaForms.stream().anyMatch(this::hasProperty);
    }

    public Set<String> getLemmaInfo(String text) {
        String[] words = getRussianWords(text);
        TreeSet<String> lemmaInfo = new TreeSet<>();

        for (String word : words) {
            if (!word.isBlank()) {
                List<String> wordNormalForms = luceneMorph.getNormalForms(word);
                wordNormalForms.forEach(normalForm -> {
                    List<String> info = luceneMorph.getMorphInfo(normalForm);
                    info.forEach(i -> {
                        String[] prop = i.split(" ");
                        //lemmaInfo.add(i +" -> "+prop[1]);
                        lemmaInfo.add(prop[1]);
                    });
                });
            }
        }
        return lemmaInfo;
    }

    public HashMap<String, Integer> getLemmaCount(String text) {

        String[] words = getRussianWords(text);

        HashMap<String, Integer> lemmaHashMap = new HashMap<>();

        for (String word : words) {
            if (!word.isBlank()) {
                List<String> wordNormalForms = luceneMorph.getNormalForms(word);
                wordNormalForms.forEach(normalForm -> {
                    //List<String> info = luceneMorph.getMorphInfo(normalForm);
                    if (includeForm(normalForm)) {
                        //lemmaHashMap.put(word + "->" + normalForm + " -> " + info, 1);
                        Integer count = 1;
                        if (lemmaHashMap.containsKey(normalForm)) {
                            count = count + lemmaHashMap.get(normalForm);
                        }
                        lemmaHashMap.put(normalForm, count);
                    }
                });
            }
        }
        return lemmaHashMap;
    }

    public HashMap<String, LemmaInfo> getLemmaCountRankHashMap(String text, float weight) {

        String[] words = getRussianWords(text);

        HashMap<String, LemmaInfo> lemmaHashMap = new HashMap<>();

        for (String word : words) {
            if (!word.isBlank()) {
                List<String> wordNormalForms = luceneMorph.getNormalForms(word);
                wordNormalForms.forEach(normalForm -> {
                    boolean includeForm = true;
                    if (!(excludeList == null)) {
                        includeForm = includeForm(normalForm);
                    }
                    if (includeForm) {
                        Integer count = 1;
                        if (lemmaHashMap.containsKey(normalForm)) {
                            count = count + lemmaHashMap.get(normalForm).count;
                        }
                        lemmaHashMap.put(normalForm, new LemmaInfo(normalForm, count, count * weight));
                    }
                });
            }
        }
        return lemmaHashMap;
    }

    @Getter
    @AllArgsConstructor
    public static class LemmaInfo {
        private String lemma;
        private Integer count;
        private Float rank;
    }
    BiFunction<LemmaInfo, LemmaInfo, LemmaInfo> SUM_LEMMA_PROPERTIES = (l1, l2) ->
            new LemmaInfo(l1.getLemma(),l1.getCount() + l2.getCount(), l1.getRank() + l2.getRank());

    //Проверить
    public HashMap<String, LemmaInfo> mergeAllHashMaps(List<HashMap<String, LemmaInfo>> listHashMaps) {
        HashMap<String, LemmaInfo> result = new HashMap<>(listHashMaps.get(0));

        for (int i = 1; i < listHashMaps.size();i++) {
            listHashMaps.get(i).forEach((k, v) -> {
                result.merge(k, v, SUM_LEMMA_PROPERTIES);
            });
        }
        return result;
    }
    public List<HashMap<String, LemmaInfo>> getHashMapsLemmaForEachCssSelector(String content) {
        List<HashMap<String, LemmaInfo>> htmlFields = new ArrayList<>();
        Document document = Jsoup.parseBodyFragment(content);

        for (Field cssSelector : cssSelectors) {
            htmlFields.add(getLemmaCountRankHashMap(document.select(cssSelector.getSelector()).toString(),
                    cssSelector.getWeight()));
        }
        return htmlFields;
    }


}
