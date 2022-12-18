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
import engine.service.Lemmatization;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static engine.service.HtmlParsing.getRussianWords;

@Getter
public class LemmaComponent {
    private static PartOfSpeechRepository partOfSpeechRepository;
    private VerticalLayout mainLayout;
    private final Grid<PartsOfSpeech> gridPartsOfSpeech = new Grid<>();
    private final TextArea resultTextArea = new TextArea("Результаты морфологического анализа");
    private final TextArea textArea = new TextArea("Текст для морфологического анализа");
    private final HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();

    public LemmaComponent() {
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Лемматизатор", "xl", null));
        createTabs(List.of("Части речи", "Леммы", "Лемматизатор"));
    }
    public static void setPartOfSpeechRepository(PartOfSpeechRepository repository) {
        partOfSpeechRepository = repository;
    }
    private VerticalLayout createPartOfSpeechContent() {
        var vLayout = new VerticalLayout();
        vLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);

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
        }).setHeader("Вкл").setAutoWidth(true)
                .setSortable(false

                )
                .setWidth("10%")
                .setTextAlign(ColumnTextAlign.CENTER);
        gridPartsOfSpeech.addColumn(PartsOfSpeech::getName)
                .setHeader("Наименование").setAutoWidth(true).setSortable(true);
        gridPartsOfSpeech.addColumn(PartsOfSpeech::getShortName)
                .setHeader("Признак").setAutoWidth(true).setSortable(true);

        vLayout.add(gridPartsOfSpeech);
        return vLayout;
    }

    private HorizontalLayout createButtons() {

        var startButton = new Button("Леммы");
        startButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        startButton.addClickListener(event -> {
                    List<String> excludeList = partOfSpeechRepository.findByInclude(false)
                            .stream()
                            .map(p -> p.getShortName())
                            .collect(Collectors.toList());

                    Lemmatization lemmatizator = new Lemmatization(excludeList, null);

                    StringBuilder stringBuilder = new StringBuilder();

                    lemmatizator.getLemmaCount(textArea.getValue()).entrySet().forEach(x -> {
                        stringBuilder.append(x.getKey() + " -> " + x.getValue() + "\n");
                    });
                    resultTextArea.setValue(stringBuilder.toString());
                }
        );

        var splitButton = new Button("Разделить на слова");
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

        Button infoButton = new Button("Части речи");
        infoButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        infoButton.addClickListener(event -> {

            List<String> excludeList = partOfSpeechRepository.findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());

            Lemmatization lemmatizator = new Lemmatization(excludeList, null);
            StringBuilder stringBuilder = new StringBuilder();
            lemmatizator.getLemmaInfo(textArea.getValue()).forEach(l -> {
                stringBuilder.append(l + '\n');
                //partOfSpeechRepository.save(new PartsOfSpeech(l,true));

            });
            resultTextArea.setValue(stringBuilder.toString());
        });

        return new HorizontalLayout(splitButton, infoButton, startButton);

    }
    private VerticalLayout createLemmatisatorContent() {
        var verticalLayout = new VerticalLayout();
        verticalLayout.setAlignItems(FlexComponent.Alignment.START);

        var controlLayout = createButtons();
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
            CreateUI.hideAllVerticalLayouts(mainLayout);
            VerticalLayout content;
            switch (label) {
                case "Лемматизатор" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createLemmatisatorContent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }
                }

                case "Части речи" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createPartOfSpeechContent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }
                    gridPartsOfSpeech.setItems(partOfSpeechRepository.findAll());
                }
                case "Леммы" -> {
                    //CreateUI.hideAllVerticalLayouts(mainLayout);
                }
            }
            contentsHashMap.get(label).setVisible(true);
        });

//        VerticalLayout cont = createLemmatisatorContent();
//        contentsHashMap.put("Лематизатор", cont);
//        mainLayout.add(cont);

        VerticalLayout cont = createPartOfSpeechContent();
        contentsHashMap.put("Части речи", cont);
        mainLayout.add(cont);

        CreateUI.hideAllVerticalLayouts(mainLayout);

//        VerticalLayout activeComponent = contentsHashMap.get("Лематизатор");
//        activeComponent.setVisible(true);

        VerticalLayout activeComponent = contentsHashMap.get("Части речи");
        activeComponent.setVisible(true);
        gridPartsOfSpeech.setItems(partOfSpeechRepository.findAll());
    }
}
