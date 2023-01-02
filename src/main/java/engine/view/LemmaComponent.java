package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
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
import engine.entity.Page;
import engine.entity.PartsOfSpeech;
import engine.service.BeanAccess;
import engine.service.HtmlParsing;
import engine.service.Lemmatization;
import lombok.Getter;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static engine.service.HtmlParsing.getRussianWords;

@Getter
public class LemmaComponent {
    private final VerticalLayout mainLayout;
    private final Grid<PartsOfSpeech> gridPartsOfSpeech = new Grid<>();
    private final TextArea resultTextArea = new TextArea("Результаты морфологического анализа");
    private final TextArea textArea = new TextArea("Текст для морфологического анализа");
    private final HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();
    private final BeanAccess beanAccess;
    private final ComboBox<String> sourceSelectComboBox = new ComboBox<>("Источник:");

    public LemmaComponent(BeanAccess beanAccess) {
        this.beanAccess = beanAccess;
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Лемматизатор", "xl", null));
        createTabs(List.of("Лемматизатор", "Леммы"));
        sourceSelectComboBox.setItems("Internet", "Database");
        sourceSelectComboBox.setValue("Database");
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

//                    lemmatizator.getLemmaCount(textArea.getValue()).entrySet().forEach(x -> {
//                        stringBuilder.append(x.getKey() + " -> " + x.getValue() + "\n");
//                    });
                    lemmatizator.getLemmaCount(textArea.getValue()).forEach((key, value) ->
                            stringBuilder.append(key + " -> " + value + "\n"));

                    resultTextArea.setValue(stringBuilder.toString());
                }
        );

        var splitButton = new Button("Разделить на слова");
        splitButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        splitButton.addClickListener(event -> {
            var stringBuilder = new StringBuilder();
            String[] words = getRussianWords(textArea.getValue());
            for (String word : words) {
                if (!word.isBlank())
                    stringBuilder.append("-").append(word).append("\n");
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
                stringBuilder.append(l).append('\n');
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

    private void removeComponentById(String tabCaption) {
        VerticalLayout content = contentsHashMap.get(tabCaption);
        content.getChildren().forEach(component -> {
            component.getId().ifPresent(id -> {
                if (id.equals("VerticalLayoutForGrids")) {
                    content.remove(component);
                }
            });

        });
    }


    private Document getDocumentFromDataBase() {

        return null;
    }

    private ComboBox<Page> createPageComboBox() {
        ComboBox<Page> pageComboBox = new ComboBox<>("Адрес страницы");
        pageComboBox.setItemLabelGenerator(Page::getPath);
        pageComboBox.setItems(query -> {
            return beanAccess.getPageRepository().findAll(
                    PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("path"))
            ).stream();
        });
        return pageComboBox;
    }

    private VerticalLayout createSearchLemmaComponent() {

        ComboBox<Page> pageComboBox = createPageComboBox();
        pageComboBox.setWidth("100%");

        TextField researchUrlTextField = new TextField("Адрес страницы: ");
        Button researchButton = new Button("Загрузить");

        researchButton.addClickListener(buttonClickEvent -> {

            removeComponentById("Леммы");

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

            if (sourceSelectComboBox.getValue() == "Database") {
                String searchPath = researchUrlTextField.getValue();
                content = beanAccess.getPageRepository().getContentByPath(searchPath);
            } else
                try {
                    document = HtmlParsing.getHtmlDocument(researchUrlTextField.getValue());
                    content = document.toString();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            var listCssSelectorsHashMap =
                    lemmatizator.getHashMapsLemmaForEachCssSelector(content);


            VerticalLayout verticalLayout = new VerticalLayout();
            verticalLayout.setId("VerticalLayoutForGrids");
            verticalLayout.setWidthFull();

            HorizontalLayout hLayoutForGrids = new HorizontalLayout();
            hLayoutForGrids.setWidthFull();

            int i = 0;
            while (i < cssSelectors.size()) {
                //создаём grid для css
                hLayoutForGrids.add(createCSSGrid("CSS-селектор: [" + cssSelectors.get(i).getName() + "]",
                        listCssSelectorsHashMap.get(i).values()));
                i++;
                if ((i & 1) == 0) {
                    verticalLayout.add(hLayoutForGrids);
                    hLayoutForGrids = new HorizontalLayout();
                    hLayoutForGrids.setWidthFull();
                }
                if ((i & 1) == 0)
                    verticalLayout.add(hLayoutForGrids);
            }
            contentsHashMap.get("Леммы").add(verticalLayout);
        });

        var verticalLayout = new VerticalLayout();
        verticalLayout.setAlignItems(FlexComponent.Alignment.START);

        var hLayout = new HorizontalLayout();
        hLayout.setWidthFull();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        hLayout.add(researchUrlTextField);
        researchUrlTextField.setWidth("50%");

        hLayout.add(sourceSelectComboBox, researchButton);
        sourceSelectComboBox.setWidth("25%");
        researchButton.setWidth("25%");

        verticalLayout.add(pageComboBox, hLayout);

        return verticalLayout;
    }

    private Grid<Lemmatization.LemmaInfo> createCSSGrid(String cssSelector, Collection<Lemmatization.LemmaInfo> values) {
        DecimalFormat decimalFormat = new DecimalFormat("#,###.##");

        Grid<Lemmatization.LemmaInfo> grid = new Grid<>(Lemmatization.LemmaInfo.class, false);

        Grid.Column<Lemmatization.LemmaInfo> col1 = grid.addColumn("lemma")
                .setHeader("Lemma")
                .setTextAlign(ColumnTextAlign.START);
        Grid.Column<Lemmatization.LemmaInfo> col2 = grid.addColumn("count")
                .setHeader("Count")
                .setTextAlign(ColumnTextAlign.CENTER)
                .setFooter(createLemmaCountFooterText(values));  //Footer для column
        Grid.Column<Lemmatization.LemmaInfo> col3 =
                grid.addColumn(new NumberRenderer<>(Lemmatization.LemmaInfo::getRank, decimalFormat)).setHeader("Rank");

        HeaderRow headerRow = grid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText(cssSelector);
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(col1, col2, col3).setComponent(simpleCell);

        grid.setItems(values);

        return grid;
    }

    private static String createLemmaCountFooterText(Collection<Lemmatization.LemmaInfo> listLemmaInfo) {

        //Сумма всех лемм с повторениями
        Optional<Integer> lemmaCount = listLemmaInfo
                .stream()
                .map(Lemmatization.LemmaInfo::getCount)
                .reduce((a, b) -> a + b);

        if (lemmaCount.isPresent())
            return String.format("Всего: %s", lemmaCount.get());
        return "";

        //Количество уникальных лемм:
//        return "Леммы:".concat(String.valueOf(listLemmaInfo.size()));

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

        var cont = createLemmatisatorContent();
        contentsHashMap.put("Лематизатор", cont);
        mainLayout.add(cont);

        CreateUI.hideAllVerticalLayouts(mainLayout);

        VerticalLayout activeComponent = contentsHashMap.get("Лематизатор");
        activeComponent.setVisible(true);
    }
}
