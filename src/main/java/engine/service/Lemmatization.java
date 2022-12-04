package engine.service;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static engine.service.HtmlParsing.getRussianWords;

public class Lemmatization {
    private final LuceneMorphology luceneMorph;
    private List<String> excludeList;

    public Lemmatization(List<String> excludeList) {
        this.excludeList = excludeList;
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

}
