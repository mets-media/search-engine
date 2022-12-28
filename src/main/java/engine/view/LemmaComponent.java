package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.NumberRenderer;
import engine.entity.Field;
import engine.entity.PartsOfSpeech;
import engine.repository.PartOfSpeechRepository;
import engine.service.BeanAccess;
import engine.service.HtmlParsing;
import engine.service.Lemmatization;
import lombok.Getter;
import org.jsoup.nodes.Document;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static engine.service.HtmlParsing.getRussianWords;

@Getter
public class LemmaComponent {
    //private static PartOfSpeechRepository partOfSpeechRepository;
    private VerticalLayout mainLayout;
    private final Grid<PartsOfSpeech> gridPartsOfSpeech = new Grid<>();
    private final TextArea resultTextArea = new TextArea("Результаты морфологического анализа");
    private final TextArea textArea = new TextArea("Текст для морфологического анализа");
    private final HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();
    private final BeanAccess beanAccess;

    public LemmaComponent(BeanAccess beanAccess) {
        this.beanAccess = beanAccess;
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Лемматизатор", "xl", null));
        createTabs(List.of("Части речи", "Леммы", "Лемматизатор"));
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
                        beanAccess.getPartOfSpeechRepository().save(item);
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
                    List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
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

            List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
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

    private VerticalLayout createSearchLemmaComponent() {

        TextField researchUrlTextField = new TextField("url: ");
        Button researchButton = new Button("Загрузить");

        researchButton.addClickListener(buttonClickEvent -> {
            if (researchUrlTextField.isEmpty())
                return;

            List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                    .stream()
                    .map(PartsOfSpeech::getShortName)
                    .collect(Collectors.toList());

            List<Field> cssSelectors = beanAccess.getFieldRepository().findByActive(true);

            //Создаём лемматизатор с указанием excludeList & cssSelectors
            Lemmatization lemmatizator = new Lemmatization(excludeList, cssSelectors);

            Document document = null;
            String content = "";
            try {
                document = HtmlParsing.getHtmlDocument(researchUrlTextField.getValue());
                content = document.body().toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {

                var listCssSelectorsHashMap =
                        lemmatizator.getHashMapsLemmaForEachCssSelector(content);


                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidthFull();

                HorizontalLayout hLayoutForGrids = null;

                int i =0;
                while (i < cssSelectors.size()) {
                    if ((i & 1) == 0) {
                        if (!(hLayoutForGrids == null))
                            verticalLayout.add(hLayoutForGrids);

                        hLayoutForGrids = new HorizontalLayout();
                        hLayoutForGrids.setWidthFull();
                    }

                    hLayoutForGrids.add(createCSSGrid("CSS-селектор: [" + cssSelectors.get(i).getName() + "]",
                            listCssSelectorsHashMap.get(i).values()));
                    i++;
                }

                if ((i & 1) == 1)
                    verticalLayout.add(hLayoutForGrids);


                contentsHashMap.get("Леммы").add(verticalLayout);
            }


        });

        var verticalLayout = new VerticalLayout();
        verticalLayout.setAlignItems(FlexComponent.Alignment.START);

        var hLayout = new HorizontalLayout();
        hLayout.setWidthFull();
        //hLayout.setHeightFull();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        hLayout.add(researchUrlTextField);
        researchUrlTextField.setWidth("75%");

        hLayout.add(researchButton);
        researchButton.setWidth("25%");

        verticalLayout.add(hLayout);

        return verticalLayout;
    }

    private Grid<Lemmatization.LemmaInfo> createCSSGrid(String cssSelector, Collection<Lemmatization.LemmaInfo> values) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###.##");

        Grid<Lemmatization.LemmaInfo> grid = new Grid<>(Lemmatization.LemmaInfo.class, false);

        Grid.Column<Lemmatization.LemmaInfo> col1 = grid.addColumn("lemma").setHeader("Lemma").setTextAlign(ColumnTextAlign.START);
        Grid.Column<Lemmatization.LemmaInfo> col2 = grid.addColumn("count").setHeader("Count").setTextAlign(ColumnTextAlign.CENTER);
        Grid.Column<Lemmatization.LemmaInfo> col3 = grid.addColumn(new NumberRenderer<>(Lemmatization.LemmaInfo::getRank, decimalFormat)).setHeader("Rank");

        HeaderRow headerRow = grid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText(cssSelector);
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(col1, col2, col3).setComponent(simpleCell);

        grid.setItems(values);

        return grid;

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
                    gridPartsOfSpeech.setItems(beanAccess.getPartOfSpeechRepository().findAll());
                }
                case "Леммы" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createSearchLemmaComponent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                        content.setSizeFull();
                    }
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
        gridPartsOfSpeech.setItems(beanAccess.getPartOfSpeechRepository().findAll());
    }
}
