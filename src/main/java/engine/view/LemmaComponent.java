package engine.view;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
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
import engine.entity.Site;
import engine.service.BeanAccess;
import engine.service.HtmlParsing;
import engine.service.Lemmatization;
import engine.service.Parser;
import lombok.Getter;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static engine.service.HtmlParsing.getRussianWords;

@Getter
public class LemmaComponent {
    private final VerticalLayout mainLayout;
    //private final Grid<PartsOfSpeech> gridPartsOfSpeech = new Grid<>();
    private final HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();
    private final BeanAccess beanAccess;
    private final ComboBox<String> sourceSelectComboBox = new ComboBox<>("Источник:");

    public LemmaComponent(BeanAccess beanAccess) {
        this.beanAccess = beanAccess;
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Лемматизатор", "xl", null));
        createTabs(List.of("Леммы", "Лемматизатор"));
        sourceSelectComboBox.setItems("Internet", "Database");
        sourceSelectComboBox.setValue("Database");
    }


    private HorizontalLayout createButtonAndLayout(TextArea textArea) {
        //-------------------------------------------------------------------------------------------------------------

        var splitButton = new Button("Слова, Леммы, Части речи");
        splitButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        splitButton.addClickListener(event -> {

            removeComponentById(contentsHashMap.get("Лемматизатор"), "HorizontalLayoutForGrids");

//            var stringBuilder = new StringBuilder();
            String[] words = getRussianWords(textArea.getValue());
//            for (String word : words) {
//                if (!word.isBlank())
//                    stringBuilder.append("-").append(word).append("\n");
//            }

            var horizontalLayout = new HorizontalLayout();
            horizontalLayout.setWidthFull();
            horizontalLayout.setId("HorizontalLayoutForGrids");
            horizontalLayout.add(getStringGrid("Слова", Arrays.stream(words).toList()));


            List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());

            Lemmatization lemmatizator = new Lemmatization(excludeList, null);


            var hm = lemmatizator.getLemmaCountRankHashMap(textArea.getValue(), 1);

            var grid = createCSSGrid("Леммы", hm.values());
            grid.getColumns().get(2).setVisible(false);
            horizontalLayout.add(grid);

            horizontalLayout.add(getStringGrid("Части речи",
                    lemmatizator.getLemmaInfo(textArea.getValue()).stream().toList()));

            contentsHashMap.get("Лемматизатор").add(horizontalLayout);

        });

        //-------------------------------------------------------------------------------------------------------------
        return new HorizontalLayout(splitButton);

    }

    private VerticalLayout createLemmatisatorContent() {

        TextArea textArea = new TextArea("Текст");
        textArea.setHeight("90%");

        var verticalLayout = new VerticalLayout();
        verticalLayout.setAlignItems(FlexComponent.Alignment.END);

        var controlLayout = createButtonAndLayout(textArea);
        controlLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        controlLayout.setAlignItems(FlexComponent.Alignment.END);
        controlLayout.setSizeUndefined();

        var hLayout = new HorizontalLayout();
        hLayout.setWidthFull();
        hLayout.setHeight(220, Unit.PIXELS);
        //hLayout.setHeight("15%");
        hLayout.add(textArea);
        textArea.setWidth("100%");


        verticalLayout.add(hLayout, controlLayout);

        return verticalLayout;
    }

    private void removeComponentById(VerticalLayout container, String deleteId) {

        container.getChildren().forEach(component -> {
            component.getId().ifPresent(id -> {
                if (id.equals(deleteId)) {
                    container.remove(component);
                }
            });
        });
    }

    private ComboBox<Page> createPageComboBox(TextField researchUrlTextField) {
        ComboBox<Page> pageComboBox = new ComboBox<>("Поиск страниц по фильтру в базе данных");
        pageComboBox.setClearButtonVisible(true);

        pageComboBox.setItems(query -> {
            return beanAccess.getPageRepository().findAll(
                    PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("path"))
            ).stream();
        });


        pageComboBox.setItemLabelGenerator(Page::getPath);

        pageComboBox.addValueChangeListener(event -> {
            if (!(event.getValue() == null))
                researchUrlTextField.setValue(event.getValue().getPath());
        });
        return pageComboBox;
    }

    BiFunction<Lemmatization.LemmaInfo, Lemmatization.LemmaInfo, Lemmatization.LemmaInfo> SUB_LEMMA_PROPERTIES =
            (l1, l2) -> new Lemmatization.LemmaInfo(l1.getLemma(),
                    l1.getCount() - l2.getCount(),
                    l1.getRank() - l2.getCount() * (l1.getRank() / l1.getCount()));

    private Button createGetLemmaInfoButton(TextField urlTextField) {
        Button getLemmaButton = new Button("Найти леммы");
        getLemmaButton.setIcon(VaadinIcon.TWIN_COL_SELECT.create());

        getLemmaButton.addClickListener(buttonClickEvent -> {

            removeComponentById(contentsHashMap.get("Леммы"), "VerticalLayoutForGrids");

            if (urlTextField.isEmpty())
                return;
            //----------------------------------------------------------------------------------------------------
            List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                    .stream()
                    .map(PartsOfSpeech::getShortName)
                    .collect(Collectors.toList());

            List<Field> cssSelectors = beanAccess.getFieldRepository().findByActive(true);

            //Создаём лемматизатор с указанием excludeList & cssSelectors
            Lemmatization lemmatizator = new Lemmatization(excludeList, cssSelectors);
            //----------------------------------------------------------------------------------------------------

            Document document = null;
            String content = "";

            if (sourceSelectComboBox.getValue() == "Database") {
                String searchPath = urlTextField.getValue();
                content = beanAccess.getPageRepository().getContentByPath(searchPath);
                if (content == null) {
                    CreateUI.showMessage("Страница отсутствует в базе данных", 2000, Notification.Position.MIDDLE);
                    return;
                }
            } else
                try {
                    document = HtmlParsing.getHtmlDocument(urlTextField.getValue());
                    content = document.toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            var listCssSelectorsHashMap =
                    lemmatizator.getHashMapsLemmaForEachCssSelector(content);

            if (listCssSelectorsHashMap.size() > 2) {
                //Надо вычесть из css[body] леммы дополнительных css селеторов
                //Данные [body]
                HashMap<String, Lemmatization.LemmaInfo> newBodyHashMap = new HashMap<>(listCssSelectorsHashMap.get(1));

                for (int i = 2; i < listCssSelectorsHashMap.size(); i++) {
                    listCssSelectorsHashMap.get(i).forEach((k, v) -> newBodyHashMap.merge(k, v, SUB_LEMMA_PROPERTIES));
                }
                //Перезаписываем изменённый HashMap для css[body]
                listCssSelectorsHashMap.set(1, newBodyHashMap);
            }

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
        return getLemmaButton;
    }

    private TextField createFilterTextField(ComboBox<Page> pageComboBox) {
        TextField filterTextField = new TextField("Фильтр");
        //filterTextField.setPlaceholder("Фильтр");
        filterTextField.setPrefixComponent(VaadinIcon.SEARCH.create());

        filterTextField.addValueChangeListener(event -> pageComboBox.setItems(query -> {
            return beanAccess.getPageRepository().findByPathContainingOrderByPath(event.getValue(),
                    PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("path"))
            ).stream();
        }));

        return filterTextField;
    }

    private TextField createResearchTextField() {
        TextField researchUrlTextField = new TextField("Адрес страницы: ");

        researchUrlTextField.setPrefixComponent(VaadinIcon.FILE_SEARCH.create());

        researchUrlTextField.addValueChangeListener(event -> {
            removeComponentById(contentsHashMap.get("Леммы"), "VerticalLayoutForGrids");
        });

        return researchUrlTextField;
    }

    private Button createBrowserButton(ComboBox<Page> pageComboBox) {
        Button button = new Button();
        button.setIcon(VaadinIcon.BROWSER.create());
        button.getElement().setProperty("title", "Открыть в браузере");

        button.addClickListener(event -> StartBrowser.startBrowser(pageComboBox.getValue().getPath()));
        return button;
    }

    private Button createDelPageButton(TextField urlTextField) {
        Button button = new Button();
        button.setIcon(VaadinIcon.DEL_A.create());
        button.getElement().setProperty("title", "Удалить страницу из базы");

        button.addClickListener(event -> {
            beanAccess.getPageRepository().deleteByPath(urlTextField.getValue());
            CreateUI.showMessage("Страница удалена из базы", 1000, Notification.Position.MIDDLE);
            removeComponentById(contentsHashMap.get("Леммы"), "VerticalLayoutForGrids");
        });
        return button;
    }

    private Button createReIndexPageButton(TextField urlTextField) {
        Button button = new Button();
        button.setIcon(VaadinIcon.ADD_DOCK.create());
        button.getElement().setProperty("title", "Добавить в базу и проиндексировать");

        button.addClickListener(event -> {

            String path = urlTextField.getValue();

            Site findSite = null;
            for (Site site : beanAccess.getSiteRepository().findAll()) {
                if (path.contains(HtmlParsing.getDomainName(site.getUrl()))) {
                    findSite = site;
                    break;
                }
            }

            AtomicBoolean pageInserted = new AtomicBoolean();
            if (!(findSite == null)) {
                Site finalFindSite = findSite;
                new Thread(() -> {
                    try {
                        if (Parser.indexingPage(finalFindSite, path, beanAccess))
                            pageInserted.set(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } else {
                CreateUI.showMessage("Страница за пределами проиндексированных сайтов!",
                        2000,
                        Notification.Position.MIDDLE);
            }
            if (pageInserted.get())
                CreateUI.showMessage("Страница загружена и проиндексирована!",
                        2000, Notification.Position.MIDDLE);
        });
        return button;
    }

    private VerticalLayout createSearchLemmaComponent() {

        TextField urlTextField = createResearchTextField();
        //------------------------------------------------------------------------------------

        ComboBox<Page> pageComboBox = createPageComboBox(urlTextField);
        TextField filterTextField = createFilterTextField(pageComboBox);
        Button browserButton = createBrowserButton(pageComboBox);

        HorizontalLayout filterHLayout = new HorizontalLayout(filterTextField, pageComboBox, browserButton);
        filterHLayout.setWidthFull();
        filterHLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        filterTextField.setWidth("30%");
        pageComboBox.setWidth("70%");

        //------------------------------------------------------------------------------------

        Button getLemmaButton = createGetLemmaInfoButton(urlTextField);

        var hLayout = new HorizontalLayout();
        hLayout.setWidthFull();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        hLayout.add(urlTextField, createDelPageButton(urlTextField), createReIndexPageButton(urlTextField), sourceSelectComboBox, getLemmaButton);

        urlTextField.setWidth("50%");
        sourceSelectComboBox.setWidth("25%");
        getLemmaButton.setWidth("25%");
        //------------------------------------------------------------------------------------

        var verticalLayout = new VerticalLayout();
        verticalLayout.setAlignItems(FlexComponent.Alignment.START);
        verticalLayout.add(filterHLayout, hLayout);

        return verticalLayout;
    }

    private Grid<String> getStringGrid(String caption, List<String> words) {
        Grid<String> grid = new Grid<>(String.class, false);
        Grid.Column<String> col1 = grid.addColumn(String::toString)
                .setHeader(caption)
                .setTextAlign(ColumnTextAlign.START)
                .setFooter(createWordsCountFooterText(words));

        grid.setItems(words);
        return grid;
    }

    private Grid<String> getStringGridWithHeader(String caption, List<String> words) {
        Grid<String> grid = new Grid<>(String.class, false);
        Grid.Column<String> col1 = grid.addColumn(String::toString).setHeader("Имя колонки").setTextAlign(ColumnTextAlign.START);
        Grid.Column<String> col2 = grid.addColumn(String::toString).setHeader("Word2");
        col2.setVisible(false);
        grid.setItems(words);

        HeaderRow headerRow = grid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText(caption);
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(col1, col2).setComponent(simpleCell);
        return grid;
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
                grid.addColumn(new NumberRenderer<>(Lemmatization.LemmaInfo::getRank, decimalFormat))
                        .setHeader("Rank");
                        //.setFooter(createRankSumFooterText(values));

        HeaderRow headerRow = grid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText(cssSelector);
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(col1, col2, col3).setComponent(simpleCell);

        grid.setItems(values);

        return grid;
    }

    private static String createWordsCountFooterText(List<String> words) {
        return "Всего: " + words.size();
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

    private static String createRankSumFooterText(Collection<Lemmatization.LemmaInfo> listLemmaInfo) {
        Optional<Float> rankSum = listLemmaInfo
                .stream()
                .map(Lemmatization.LemmaInfo::getRank)
                .reduce((a, b) -> a + b);
        if (rankSum.isPresent())
            return rankSum.toString();
        return "";
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
                }
            }
            contentsHashMap.get(label).setVisible(true);
        });

        var cont = createSearchLemmaComponent();
        contentsHashMap.put("Леммы", cont);
        mainLayout.add(cont);

        CreateUI.hideAllVerticalLayouts(mainLayout);

        VerticalLayout activeComponent = contentsHashMap.get("Леммы");
        activeComponent.setVisible(true);
    }
}
