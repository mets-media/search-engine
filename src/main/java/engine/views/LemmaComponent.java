package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import engine.entity.PartsOfSpeech;
import engine.repository.PartOfSpeechRepository;
import lombok.Getter;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Getter
public class LemmaComponent {
    private PartOfSpeechRepository partOfSpeechRepository;
    private final LuceneMorphology luceneMorph;
    private List<String> excludeList;
    private final VerticalLayout mainLayout;
    private final Grid<PartsOfSpeech> gridPartsOfSpeech = new Grid<>();
    private final TextArea resultTextArea = new TextArea("Результаты морфологического анализа");
    private final TextArea textArea = new TextArea("Текст для морфологического анализа");
    private final HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();

    public LemmaComponent() {
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Настройки лемматизатора", null));

        createTabs(List.of("Лемматизатор", "Части речи", "Леммы"));


        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPartOfSpeechRepository(PartOfSpeechRepository partOfSpeechRepository) {
        this.partOfSpeechRepository = partOfSpeechRepository;
    }

    private VerticalLayout createPartOfSpeechContent() {
        var vLayout = new VerticalLayout();

        var startButton = new Button("Все части речи");
        startButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        startButton.addClickListener(event -> {
        });

        gridPartsOfSpeech.addThemeVariants(GridVariant.LUMO_COMPACT);
        gridPartsOfSpeech.setSelectionMode(Grid.SelectionMode.SINGLE);

        gridPartsOfSpeech.addComponentColumn(item -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(item.getInclude());
            checkbox.addValueChangeListener(event -> {
                item.setInclude(event.getValue());
                partOfSpeechRepository.save(item);
            });
            return checkbox;
        }).setHeader("Вкл.в отчёт").setAutoWidth(true).setSortable(true).setTextAlign(ColumnTextAlign.CENTER);
        gridPartsOfSpeech.addColumn(PartsOfSpeech::getName)
                .setHeader("Наименование").setAutoWidth(true).setSortable(true);
        gridPartsOfSpeech.addColumn(PartsOfSpeech::getShortName)
                .setHeader("Сокращение").setAutoWidth(true).setSortable(true);

        vLayout.add(gridPartsOfSpeech);

        return vLayout;
    }

    private void hideAllVerticalLayouts() {
        mainLayout.getChildren().forEach(component -> {
            if (component.getClass() == VerticalLayout.class)
                component.setVisible(false);
        });
    }

    private VerticalLayout createLemmatisatorContent() {
        var verticalLayout = new VerticalLayout();
        verticalLayout.setAlignItems(FlexComponent.Alignment.START);

        var startButton = new Button("Start");
        startButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        startButton.addClickListener(event -> {
                    excludeList = partOfSpeechRepository.findByInclude(false)
                            .stream()
                            .map(p -> p.getShortName())
                            .collect(Collectors.toList());

                    StringBuilder stringBuilder = new StringBuilder();
                    getLemmaCount(textArea.getValue()).entrySet().forEach(x -> {
                        //stringBuilder.append(x.getKey() + " -> " + x.getValue() + "\n");
                        stringBuilder.append(x.getKey() + " -> " + x.getValue() + "\n");
                    });
                    resultTextArea.setValue(stringBuilder.toString());
                }
        );

        var splitButton = new Button("Split");
        splitButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        splitButton.addClickListener(event -> {
            StringBuilder stringBuilder = new StringBuilder();
            String[] words = getRussianWords(textArea.getValue());
            for (String word : words) {
                if (!word.isBlank())
                    stringBuilder.append("-" + word + "\n");
            }
            resultTextArea.setValue(stringBuilder.toString());
        });

        Button infoButton = new Button("Info");
        infoButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        infoButton.addClickListener(event -> {
            StringBuilder stringBuilder = new StringBuilder();
            getLemmaInfo(textArea.getValue()).forEach(l -> {
                stringBuilder.append(l + '\n');
                //partOfSpeechRepository.save(new PartsOfSpeech(l,true));

            });
            resultTextArea.setValue(stringBuilder.toString());
        });

        var controlLayout = new HorizontalLayout(splitButton, startButton, infoButton);

        controlLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        controlLayout.setAlignItems(FlexComponent.Alignment.END);
        controlLayout.setSizeUndefined();

        var hLayout = new HorizontalLayout();
        hLayout.setWidthFull();
        hLayout.setHeightFull();

        hLayout.add(textArea);
        textArea.setWidthFull();
        textArea.setHeight("100%");

        hLayout.add(resultTextArea);
        resultTextArea.setReadOnly(true);
        resultTextArea.setWidthFull();
        resultTextArea.setHeight("100%");

        verticalLayout.add(controlLayout, hLayout);

        return verticalLayout;
    }

    private void createTabs(List<String> captions) {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.HORIZONTAL);
        tabs.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        for (String caption : captions) {
            Tab newTab = new Tab(caption);
            tabs.add(newTab);
        }
        mainLayout.add(tabs);

        tabs.addSelectedChangeListener(event -> {
            String label = tabs.getSelectedTab().getLabel();
            switch (label) {
                case "Лемматизатор" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        VerticalLayout cont = createLemmatisatorContent();
                        contentsHashMap.put(label, cont);
                        mainLayout.add(cont);
                    }
                    hideAllVerticalLayouts();
                    VerticalLayout activeComponent = contentsHashMap.get(label);
                    activeComponent.setVisible(true);

                }

                case "Части речи" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        VerticalLayout cont = createPartOfSpeechContent();
                        contentsHashMap.put(label, cont);
                        mainLayout.add(cont);

                        if (partOfSpeechRepository.count() == 0) {
                            partOfSpeechRepository.initData();
                        }
                    }

                    hideAllVerticalLayouts();
                    VerticalLayout activeComponent = contentsHashMap.get(label);
                    activeComponent.setVisible(true);

                    gridPartsOfSpeech.setItems(partOfSpeechRepository.findAll());
                }
                case "Леммы" -> {
                    hideAllVerticalLayouts();
                }
            }
        });

        VerticalLayout cont = createLemmatisatorContent();
        contentsHashMap.put("Лематизатор", cont);
        mainLayout.add(cont);

        hideAllVerticalLayouts();
        VerticalLayout activeComponent = contentsHashMap.get("Лематизатор");
        activeComponent.setVisible(true);

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

    private String[] getRussianWords(String text) {
        String[] words = text.toLowerCase()
                .replaceAll("[^\\p{IsCyrillic}]", " ")
                .trim()
                .split("[\\s+]");
        return words;
    }

    private Set<String> getLemmaInfo(String text) {
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

    private HashMap<String, Integer> getLemmaCount(String text) {

        String[] words = getRussianWords(text);

        HashMap<String, Integer> lemmaHashMap = new HashMap<>();

        for (String word : words) {
            if (!word.isBlank()) {
                List<String> wordNormalForms = luceneMorph.getNormalForms(word);
                wordNormalForms.forEach(normalForm -> {
                    List<String> info = luceneMorph.getMorphInfo(normalForm);
                    if (includeForm(normalForm)) {
                        //lemmaHashMap.put(word + "->" + normalForm + " -> " + info, 1);
                        Integer count = 1;
                        if (lemmaHashMap.containsKey(normalForm)) {
                            count = count + lemmaHashMap.get(normalForm);
                        }
                        lemmaHashMap.put(normalForm, count);
                    }
                });


//                wordNormalForms.forEach(lemma -> {
//                    Integer count = 1;
//                    lemma = word + ": " + lemma;
//                    if (lemmaHasMap.containsKey(lemma)) {
//                        count = lemmaHasMap.get(lemma) + 1;
//                    }
//                    lemmaHasMap.put(lemma, count);
//                });
            }
        }
        return lemmaHashMap;
    }

}
