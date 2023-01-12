package engine.view;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import engine.entity.Lemma;
import engine.entity.PathTable;
import engine.entity.Site;
import engine.service.BeanAccess;
import engine.service.HtmlParsing;
import engine.service.Lemmatization;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SearchComponent {

    private static BeanAccess beanAccess;
    private ComboBox<Site> siteComboBox = null;
    private final HashMap<String, List<String>> pagesHashMap = new HashMap<>();
    private final HashMap<String, List<Integer>> pageIdHashMap = new HashMap<>();
    private final VerticalLayout mainLayout;
    private final HorizontalLayout requestLayout = new HorizontalLayout();
    private final HorizontalLayout gridsLayout = new HorizontalLayout();
    private final Grid<Lemma> lemmaGrid = new Grid<>(Lemma.class, false);
    private final Grid<PathTable> relevanceGrid = new Grid<>(PathTable.class, false);
    private final TextField pageCountTextField = new TextField("Страницы");
    private final TextField lemmaCountTextField = new TextField("Леммы");
    private final TextField indexCountTextField = new TextField("Таблица Index");
    private final TextField requestTextField = new TextField("Поисковый запрос");
    private final TextArea htmlTextArea = new TextArea("Snippet: <b> tag");


    public SearchComponent() {
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Система поиска", "xl", null));
        mainLayout.add(createSearchComponent());
        requestLayout.setSizeFull();
        requestTextField.setSizeFull();
        lemmaGrid.setWidth("40%");
        relevanceGrid.setWidth("60%");

        lemmaGrid.setHeight(250, Unit.PIXELS);
        relevanceGrid.setHeight(250, Unit.PIXELS);

        createColumnsRelevanceGrid();
    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    public static void setDataAccess(BeanAccess beanAccess) {
        SearchComponent.beanAccess = beanAccess;
    }

    private VerticalLayout createSearchComponent() {

        Collection<TextField> textFieldCollection = Arrays.asList(pageCountTextField, lemmaCountTextField, indexCountTextField);
        textFieldCollection.forEach(textField -> {
            textField.setReadOnly(true);
            textField.setWidth("15%");
        });

        var horizontalLayout = new HorizontalLayout(
                createSiteComboBox(),
                pageCountTextField,
                lemmaCountTextField,
                indexCountTextField);
        siteComboBox.setWidthFull();
        horizontalLayout.setAlignItems(FlexComponent.Alignment.END);
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        horizontalLayout.setSizeUndefined();

        createColumnsLemmaGrid();
        //createColumnsPageGrid();
        //createColumnsRelevanceGrid();

        requestTextField.setSizeUndefined();

        requestLayout.add(requestTextField, createSearchButton());
        requestLayout.setSizeUndefined();
        requestLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        requestLayout.setEnabled(false);

        //VerticalLayout relevanceLayout = new VerticalLayout(, relevanceGrid);

        gridsLayout.add(lemmaGrid, relevanceGrid);
        gridsLayout.setWidthFull();

        htmlTextArea.setWidthFull();
        htmlTextArea.setReadOnly(true);

        return new VerticalLayout(horizontalLayout, requestLayout,
                gridsLayout,
                htmlTextArea);
    }

    private ComboBox<Site> createSiteComboBox() {
        siteComboBox = new ComboBox<>("Сайт:");
        siteComboBox.setItemLabelGenerator(Site::getUrl);

//        siteComboBox.setItems(query -> {
//            return beanAccess.getSiteRepository().getSitesFromPageTable(
//                    PageRequest.of(query.getPage(), query.getPageSize())
//            ).stream();
//        });


        List<Site> siteList = beanAccess.getSiteRepository().getSitesFromPageTable();

        Site allSiteObject = new Site();
        allSiteObject.setId(0);
        allSiteObject.setName("*");
        allSiteObject.setUrl("Все сайты");
        siteList.add(0, allSiteObject);

        siteComboBox.setItems(siteList);

        siteComboBox.addValueChangeListener(event -> {

            lemmaGrid.setItems(new ArrayList<>());
            relevanceGrid.setItems(new ArrayList<>());
            pageIdHashMap.clear();

            switch (event.getValue().getName()) {
                case "*" -> {
/*
                    long pageCount = beanAccess.getPageRepository().count();
                    long lemmaCount = beanAccess.getLemmaRepository().count();
                    long indexCount = beanAccess.getIndexRepository().count();
*/
                    for (Site site : beanAccess.getSiteRepository().getStatistic()) {
                        beanAccess.getSiteRepository().save(site);
                    }
                    int pageCount = 0;
                    int lemmaCount = 0;
                    int indexCount = 0;
                    for (Site site : beanAccess.getSiteRepository().findAll()) {
                        pageCount += site.getPageCount();
                        lemmaCount += site.getLemmaCount();
                        indexCount += site.getIndexCount();
                    }

                    pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));
                    lemmaCountTextField.setValue(new DecimalFormat("#,###").format(lemmaCount));
                    indexCountTextField.setValue(new DecimalFormat("#,###").format(indexCount));

                }
                default -> {
/*
                    int siteId = event.getValue().getId();

                    Integer pageCount = beanAccess.getPageRepository().countBySiteId(siteId);
                    Integer lemmaCount = beanAccess.getLemmaRepository().countBySiteId(siteId);
                    Integer indexCount = beanAccess.getIndexRepository().countBySiteId(siteId);
*/
                    Site site = event.getValue();
                    Integer pageCount = site.getPageCount();
                    Integer lemmaCount = site.getLemmaCount();
                    Integer indexCount = site.getIndexCount();


                    pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));
                    lemmaCountTextField.setValue(new DecimalFormat("#,###").format(lemmaCount));
                    indexCountTextField.setValue(new DecimalFormat("#,###").format(indexCount));
                }
            }
            requestLayout.setEnabled(true);
        });
        siteComboBox.setWidth("30%");
        return siteComboBox;
    }

    private void createColumnsRelevanceGrid() {

        relevanceGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        relevanceGrid.addThemeVariants(GridVariant.LUMO_COMPACT);


        DecimalFormat decimalFormat = new DecimalFormat("#,###.##");


        Grid.Column<PathTable> absRelevanceColumn = relevanceGrid.addColumn(new NumberRenderer<>(PathTable::getAbsRelevance,
                        decimalFormat))
                .setHeader("Абсолютная")
                .setTextAlign(ColumnTextAlign.CENTER)
                .setFrozen(true);

        Grid.Column<PathTable> relRelevanceColumn = relevanceGrid.addColumn(new NumberRenderer<>(PathTable::getRelRelevance,
                        decimalFormat))
                .setHeader("Относительная")
                .setTextAlign(ColumnTextAlign.CENTER)
                .setFrozen(true);

        Grid.Column<PathTable> pathRelevanceColumn = relevanceGrid.addColumn(PathTable::getPath)
                .setHeader("Страница")
                .setTextAlign(ColumnTextAlign.START)
                .setWidth("60%").
                setResizable(true);

        HeaderRow headerRow = relevanceGrid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText("Релевантность");
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(absRelevanceColumn, relRelevanceColumn).setComponent(simpleCell);

        //Без detail
        //relevanceGrid.setItemDetailsRenderer(createPageDetailRenderer());

        relevanceGrid.addItemDoubleClickListener(event -> {
            String path = event.getItem().getPath();
            StartBrowser.startBrowser(path);
        });

        relevanceGrid.addSelectionListener(selectionEvent -> {

            selectionEvent.getFirstSelectedItem().ifPresent(t -> {
                Integer pageId = t.getPageId();

                List<Lemma> lemmaList = beanAccess.getLemmaRepository().findByPageId(pageId);

                beanAccess.getPageRepository().findById(t.getPageId()).ifPresent(page -> {
                    htmlTextArea.setValue(HtmlParsing.getBoldRussianText(page.getContent()));
                });

            });

        });
    }

    private void createColumnsLemmaGrid() {
        lemmaGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        CreateUI.setAllCheckboxVisibility(lemmaGrid, false);
        lemmaGrid.addColumn(Lemma::getFrequency).setHeader("Частота")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);
        lemmaGrid.addColumn(Lemma::getLemma).setHeader("Лемма")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.START);

        //--------------------------
        //Действие при выборе леммы
        //--------------------------
        lemmaGrid.addSelectionListener(selectionEvent -> {
            htmlTextArea.clear();

            Integer siteId = siteComboBox.getValue().getId();

            //Формирование списков страниц
            selectionEvent.getAllSelectedItems().forEach(lemma -> {
                String selectedLemma = lemma.getLemma();

                List<Integer> pageIdList;
                if (siteId == 0) //Для всех сайтов
                    pageIdList = beanAccess.getPageRepository().getPageIdByLemma(selectedLemma);
                else //Для выбранного сайта
                    pageIdList = beanAccess.getPageRepository().getPageIdBySiteIdAndLemma(selectedLemma, siteId);

                //Для каждой леммы - свой список страниц (pageId)
                if (!pageIdHashMap.containsKey(selectedLemma))
                    pageIdHashMap.put(selectedLemma, pageIdList);
            });

            //Формируем строку со всеми выбранными леммами
            StringBuilder stringBuilder = new StringBuilder();
            selectionEvent.getAllSelectedItems().forEach(l -> stringBuilder.append(l.getLemma()).append(","));
            String includeLemma = stringBuilder.toString();

            if (includeLemma.isEmpty()) {
                relevanceGrid.setItems(new ArrayList<>());
                relevanceGrid.getColumns().get(2).setHeader("Страницы");
                return;
            }
            includeLemma = "'" + includeLemma.substring(0, includeLemma.length() - 1) + "'";

            //Retain all pageId - выбираем пересечение страниц для всех лемм
            var pageIdRetained = retainAllPageId(pageIdHashMap);

            stringBuilder.delete(0, stringBuilder.length());
            pageIdRetained.forEach(pageId -> stringBuilder.append(pageId.toString()).append(","));

            //Строка с общими для всех лемм страницами
            var includePageId = stringBuilder.toString();

            if (!(includePageId.isBlank())) {
                includePageId = includePageId.substring(0, includePageId.length() - 1);

                List<PathTable> pathTableList;
                if (siteId == 0)
                    pathTableList = beanAccess.getPathTableRepository()
                            .getResultTableForAllSites(includeLemma, includePageId);
                else
                    pathTableList = beanAccess.getPathTableRepository()
                            .getResultTableForSelectedSite(siteId, includeLemma, includePageId);

                //Результаты
                relevanceGrid.setItems(pathTableList);
                relevanceGrid.getColumns().get(2).setHeader("Страниц: " + pathTableList.size());
            } else {
                relevanceGrid.setItems(new ArrayList<>());
                relevanceGrid.getColumns().get(2).setHeader("Страницы");
            }
        });
    }

    private List<String> retainAllSelectedLemmas() {

        List<Lemma> sortedLemma = lemmaGrid.getSelectedItems()
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

    private List<Integer> retainAllPageId(HashMap<String, List<Integer>> hashMap) {

        List<Lemma> sortedLemma = lemmaGrid.getSelectedItems()
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

    private Button createSearchButton() {
        Button searchButton = new Button("Найти");
        searchButton.setWidth("10%");

        searchButton.addClickListener(buttonClickEvent -> {
            setLemmaGridItems(siteComboBox.getValue().getId());
        });
        return searchButton;
    }

    private void setLemmaGridItems(Integer siteId) {
        List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                .stream()
                .map(p -> p.getShortName())
                .collect(Collectors.toList());

        Lemmatization lemmatizator = new Lemmatization(excludeList, null);

        //Все лемммы из запроса
        HashMap<String, Integer> requestLemmas = lemmatizator.getLemmaHashMap(requestTextField.getValue());

        //Integer siteId = siteComboBox.getValue().getId();
        List<String> lemmaList = requestLemmas.keySet().stream().toList();

        StringBuilder stringBuilder = new StringBuilder();
        for (String lemma : lemmaList) {
            stringBuilder.append(lemma.concat("','"));
        }
        String includeLemma = "'" + stringBuilder;
        includeLemma = includeLemma.substring(0, includeLemma.length() - 3) + "'";

        if (siteId == 0) { //Все сайты
            //Запрос в программе выдаёт неверный результат, тот же запрос в pgAdmin работает правильно
            //Причина неизвестна - БАГ!!!
            //lemmaGrid.setItems(beanAccess.getLemmaRepository().findByLemmaIn(lemmaList));

            lemmaGrid.setItems(beanAccess.getPathTableRepository().findLemmasInAllSites(includeLemma));
        } else  //Выбранный сайт
            lemmaGrid.setItems(query -> beanAccess.getLemmaRepository()
                    .findBySiteIdAndLemmaIn(
                            siteId,
                            lemmaList,
                            PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("frequency")))
                    .stream());

    }

    private static class PageDetailFormLayout extends FormLayout {
        private static final Button browserButton = new Button("Открыть страницу");
        private static final Label titleLabel = new Label("title:");


        public PageDetailFormLayout() {
            browserButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            browserButton.setWidth("40%");

            var verticalLayout = new VerticalLayout();
            verticalLayout.setAlignItems(FlexComponent.Alignment.END);
            titleLabel.setWidthFull();
            verticalLayout.add(browserButton, titleLabel);

            add(verticalLayout);
        }

        public void setPage(PathTable pathTable) {
            titleLabel.setText("title: " + pathTable.getPageId().toString());
            titleLabel.setWidthFull();

            browserButton.addClickListener(event -> {
                StartBrowser.startBrowser(pathTable.getPath());
            });

        }

    }

    private static ComponentRenderer<PageDetailFormLayout, PathTable> createPageDetailRenderer() {
        return new ComponentRenderer<>(PageDetailFormLayout::new, PageDetailFormLayout::setPage);
    }

}
