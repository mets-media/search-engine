package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import engine.entity.Lemma;
import engine.repository.LemmaRepository;
import lombok.Getter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;


@Component
@Getter
public class LemmaComponent {
    private VerticalLayout verticalLayout = new VerticalLayout();
    private HorizontalLayout horizontalLayout = new HorizontalLayout();
    private Grid grid = new Grid<>(Lemma.class, false);
    private static LemmaRepository lemmaRepository;
    private TextArea resultTextArea = new TextArea("Результаты морфологического анализа");
    private TextArea textArea = new TextArea("Текст для морфологического анализа");
    private List<String> langPart = Arrays.asList("СОЮЗ", "МЕЖД");


    public LemmaComponent() {
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);

        horizontalLayout.setWidthFull();
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        horizontalLayout.add(createControlButtons());

        HorizontalLayout horizontalLayoutText = new HorizontalLayout();
        horizontalLayoutText.setWidthFull();
        horizontalLayoutText.setHeightFull();

        horizontalLayoutText.add(textArea);
        textArea.setWidthFull();
        textArea.setHeight("100%");

        horizontalLayoutText.add(resultTextArea);
        resultTextArea.setReadOnly(true);
        resultTextArea.setWidthFull();
        resultTextArea.setHeight("100%");

        verticalLayout.add(horizontalLayout);
        verticalLayout.add(horizontalLayoutText);

        Grid grid = new Grid<>();

    }

    private HorizontalLayout createControlButtons() {
        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout horizontalLayoutForLabel = new HorizontalLayout();
        horizontalLayoutForLabel.setAlignItems(FlexComponent.Alignment.END);
        horizontalLayoutForLabel.setSizeUndefined();

        Label label = new Label("Лемматизатор");
        label.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin", "0");

        horizontalLayoutForLabel.add(label);

        Button testButton = new Button("Старт");
        testButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        testButton.addClickListener(event -> {
//            LuceneMorphology luceneMorph = null;
//            try {
//                luceneMorph = new RussianLuceneMorphology();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//
//            luceneMorph.getNormalForms("леса").forEach(System.out::println);
//
//            StringBuilder stringBuilder = new StringBuilder();
//            stringBuilder.append("Исходные формы слова \"" + textArea.getValue() + "\" :\n");
//            luceneMorph.getNormalForms(textArea.getValue().toLowerCase()).forEach(t -> {
//                stringBuilder.append('\t' + t + "\n");
//            });
//            resultTextArea.setValue(String.valueOf(stringBuilder));

            StringBuilder stringBuilder = new StringBuilder();
            getLemmaInfo(textArea.getValue()).entrySet().forEach(x -> {
                stringBuilder.append(x.getKey() + " -> " + x.getValue() + "\n");
            });
            resultTextArea.setValue(stringBuilder.toString());
        });

        Button splitButton = new Button("Split");
        splitButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        splitButton.addClickListener(event -> {
            StringBuilder stringBuilder = new StringBuilder();

            String[] words = textArea.getValue().toLowerCase().replaceAll("\\p{Punct}", " ").split("[\s\n]");

            for (String word : words) {
                if (!word.isBlank())
                    stringBuilder.append("-" + word + "\n");
            }
            resultTextArea.setValue(stringBuilder.toString());
        });

        hLayout.add(horizontalLayoutForLabel, testButton, splitButton);
        return hLayout;

    }

    private HashMap<String, Integer> getLemmaInfo(String text) {
        LuceneMorphology luceneMorph = null;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String[] words = textArea.getValue().toLowerCase()
                .replaceAll("\\p{Punct}", " ")
                .split("[\s\n]");

        HashMap<String, Integer> lemmaHasMap = new HashMap<>();

        for (String word : words) {
            if (!word.isBlank()) {
                Optional<String> wordNormalForm = luceneMorph.getNormalForms(word).stream().findFirst();
                LuceneMorphology finalLuceneMorph = luceneMorph;
                wordNormalForm.ifPresent(lemma -> {
                    String lInfo = String.valueOf(finalLuceneMorph.getMorphInfo(lemma).stream().findFirst());
                    if (!lInfo.contains("ПРЕДЛ") &&
                            !lInfo.contains("СОЮЗ") &&
                            !lInfo.contains("МЕЖД") &&
                            !lInfo.contains("ЧАСТ"))
                    {
                        Integer count = 1;
                        lemma = lemma + " " + lInfo;
                        if (lemmaHasMap.containsKey(lemma)) {
                            count = lemmaHasMap.get(lemma) + 1;
                        }
                        lemmaHasMap.put(lemma, count);
                    }
                });
            }
        }
        return lemmaHasMap;
    }
}
