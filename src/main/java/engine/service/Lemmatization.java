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
import java.util.stream.Collectors;

import static engine.service.HtmlParsing.getRussianWords;

@Getter
public class Lemmatization {
    private final LuceneMorphology luceneMorph;
    private final List<String> excludeList;
    private final List<Field> cssSelectors;

    private static BeanAccess beanAccess;

    public static void setBeanAccess(BeanAccess beanAccess) {
        Lemmatization.beanAccess = beanAccess;
    }

    public Lemmatization(List<String> excludeList, List<Field> cssSelectors) {
        this.excludeList = excludeList;
        this.cssSelectors = cssSelectors;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class LemmaInfo {
        private String lemma;
        private Integer count;
        private Float rank;
    }

    private Boolean hasProperty(String wordBaseForm) {
        for (String excludeProp : excludeList) {
            String[] form = wordBaseForm.split(" ");
            if (form[1].equals(excludeProp))
                return true;
        }
        return false;
    }

    public Boolean includeForm(String lemma) {
        List<String> lemmaForms = luceneMorph.getMorphInfo(lemma);
        return lemmaForms.stream().noneMatch(this::hasProperty);
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
                        lemmaInfo.add(prop[1]);
                    });
                });
            }
        }
        return lemmaInfo;
    }

    public HashMap<String, Integer> getLemmaHashMap(String text) {

        String[] words = getRussianWords(text);

        HashMap<String, Integer> lemmaHashMap = new HashMap<>();

        for (String word : words) {
            if (!word.isBlank()) {
                List<String> wordNormalForms = luceneMorph.getNormalForms(word);
                wordNormalForms.forEach(normalForm -> {
                    if (includeForm(normalForm)) {
                        int count = 1;
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
                        int count = 1;
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

    BiFunction<LemmaInfo, LemmaInfo, LemmaInfo> SUM_LEMMA_PROPERTIES = (l1, l2) ->
            new LemmaInfo(l1.getLemma(),l1.getCount() + l2.getCount(), l1.getRank() + l2.getRank());

    public HashMap<String, LemmaInfo> mergeAllHashMaps(List<HashMap<String, LemmaInfo>> listHashMaps) {
        HashMap<String, LemmaInfo> result = new HashMap<>(listHashMaps.get(0));

        for (int i = 1; i < listHashMaps.size();i++) {
            listHashMaps.get(i).forEach((k, v) -> result.merge(k, v, SUM_LEMMA_PROPERTIES));
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

    public static Lemmatization getLemmatizator() {
        List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                .stream()
                .map(p -> p.getShortName())
                .collect(Collectors.toList());

        Lemmatization lemmatizator = new Lemmatization(excludeList, beanAccess.getFieldRepository().findByActive(true));
        return lemmatizator;
    }
}
