package engine.view;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NumberRenderer;
import engine.entity.*;
import engine.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static engine.service.TimeMeasure.setStartTime;

public class SearchComponent {

    private static BeanAccess beanAccess;
    private ComboBox<Site> siteComboBox = null;
    //private final HashMap<String, List<String>> pagesHashMap = new HashMap<>();
    private final HashMap<String, List<Integer>> pageIdHashMap = new HashMap<>();
    private final HashMap<String, List<IndexEntity>> indexHashMap = new HashMap<>();
    private final VerticalLayout mainLayout;
    private final HorizontalLayout requestLayout = new HorizontalLayout();
    private final HorizontalLayout checkBoxAndButtonLayout = new HorizontalLayout();
    private final HorizontalLayout gridsLayout = new HorizontalLayout();
    private final Grid<Lemma> lemmaGrid = new Grid<>(Lemma.class, false);
    private final Grid<PathTable> findPageGrid = new Grid<>(PathTable.class, false);
    private final TextField pageCountTextField = new TextField("Страницы");
    private final TextField lemmaCountTextField = new TextField("Леммы");
    private final TextField indexCountTextField = new TextField("Таблица Index");
    private final TextField requestTextField = new TextField("Поисковый запрос");
    private final TextField findTextField = new TextField();
    private final Checkbox checkboxAuto = new Checkbox("Автом.расчёт");
    private final Button calcPageButton = UIElement.createButton("Расчёт релевантности", VaadinIcon.DOWNLOAD, "");
    private final VerticalLayout detailLayout = new VerticalLayout();
    private ComboBox<String> selectCountersQueryComboBox;
    private ComboBox<String> selectGetInfoQueryComboBox;
    private Site allSiteObject = new Site();
    private final Lemmatization lemmatizator;
    private String findingPages;

    public SearchComponent() {
        mainLayout = UIElement.getMainLayout();
        mainLayout.add(UIElement.getTopLayout("Система поиска", "xl", null));
        mainLayout.add(getSearchComponent());

        requestLayout.setSizeFull();
        requestTextField.setSizeFull();
        requestTextField.setPrefixComponent(VaadinIcon.SEARCH.create());
        requestTextField.setClearButtonVisible(true);

        lemmaGrid.setWidth("40%");
        findPageGrid.setWidth("60%");

        lemmaGrid.setHeight(250, Unit.PIXELS);
        findPageGrid.setHeight(250, Unit.PIXELS);

        detailLayout.setVisible(false);

        Lemmatization.setDataAccess(beanAccess);
        lemmatizator = Lemmatization.getLemmatizator();
    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    public static void setDataAccess(BeanAccess beanAccess) {
        SearchComponent.beanAccess = beanAccess;
    }

    private VerticalLayout getSearchComponent() {

        Collection<TextField> textFieldCollection = Arrays.asList(pageCountTextField, lemmaCountTextField, indexCountTextField);
        textFieldCollection.forEach(textField -> {
            textField.setReadOnly(true);
            textField.setWidth("15%");
        });

        selectCountersQueryComboBox = UIElement.createComboBox(List.of("Repository.Count", "Counters", "GetStatistic"));
        selectGetInfoQueryComboBox = UIElement.createComboBox(List.of("first variant", "second variant", "index HashMap"));
        //--------------      Сайт Страницы Леммы Index     обновить --------------
        var horizontalLayout = new HorizontalLayout(
                getSiteComboBox(),
                pageCountTextField,
                lemmaCountTextField,
                indexCountTextField,
                selectCountersQueryComboBox,
                getRefreshButton()
        );
        siteComboBox.setWidthFull();
        horizontalLayout.setAlignItems(FlexComponent.Alignment.END);
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        horizontalLayout.setSizeUndefined();

        createColumnsLemmaGrid();

        requestTextField.setSizeUndefined();

        //--------------  Текст запроса    Найти леммы ----------------------
        requestLayout.add(requestTextField, getSearchButton());
        requestLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        requestLayout.setEnabled(false);

        //-------------- Grid Леммы  --- Grid Страницы ----------------------
        gridsLayout.add(lemmaGrid, findPageGrid);
        gridsLayout.setWidthFull();


        checkboxAuto.setValue(true);
        checkboxAuto.addValueChangeListener(event -> {
            calcPageButton.setEnabled(!event.getValue());
        });
        findTextField.setWidth("50%");
        findTextField.setReadOnly(true);

        calcPageButton.setEnabled(false);
        //---------------- Вибор системы поиск, Расчёт релевантности -------------------------
        HorizontalLayout hLayout = new HorizontalLayout(selectGetInfoQueryComboBox, calcPageButton);
        hLayout.setWidth("40%");

        checkBoxAndButtonLayout.setSizeUndefined();
        checkBoxAndButtonLayout.add(hLayout, checkboxAuto, findTextField);
        checkBoxAndButtonLayout.setAlignItems(FlexComponent.Alignment.END);
        checkBoxAndButtonLayout.setWidthFull();
        checkBoxAndButtonLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        //checkBoxAndButtonLayout.setEnabled(false);


        var resultLayout = new VerticalLayout(horizontalLayout,
                requestLayout,
                checkBoxAndButtonLayout,
                gridsLayout,
                getDetailLayout());


        return resultLayout;
    }

    private Button getRefreshButton() {
        var button = UIElement.createButton("", VaadinIcon.REFRESH, "Обновить информацию");
        button.addClickListener(event -> {
            TimeMeasure.setStartTime();
            switch (selectCountersQueryComboBox.getValue()) {
                case "Counters" -> {
                    setInfoFromCounters();
                }
                case "GetStatistic" -> {
                    //Внимание!!! - долгий запрос, с записью значений для каждого сайта
                    setInfoValuesFromGetStatistic();
                }
                case "Repository.Count" -> {
                    //Установка значений путём подсчёта repository.count()
                    setInfoValuesFromRepositoryCount();
                }
            }
            UIElement.showMessage("Время запроса: " + TimeMeasure.getStringExperienceTime());

        });
        return button;
    }

    private void clearGrids() {
        lemmaGrid.setItems(new ArrayList<>());
        findPageGrid.setItems(new ArrayList<>());
        pageIdHashMap.clear();
    }

    private void setInfoValuesFromGetStatistic() {
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
        setInfoValues(pageCount, lemmaCount, indexCount);
    }

    private void setInfoValuesFromRepositoryCount() {
        long pageCount = beanAccess.getPageRepository().count();
        long lemmaCount = beanAccess.getLemmaRepository().count();
        long indexCount = beanAccess.getIndexRepository().count();

        setInfoValues(pageCount, lemmaCount, indexCount);
    }

    private void setInfoFromCounters() {
        Optional<Site> allSite = beanAccess.getSiteRepository().getAllSiteInfo();
        allSite.ifPresent(site -> {
            int pageCount = site.getPageCount();
            int lemmaCount = site.getLemmaCount();
            int indexCount = site.getIndexCount();
            setInfoValues(pageCount, lemmaCount, indexCount);
        });


    }

    private void setInfoValues(long pageCount, long lemmaCount, long indexCount) {
        pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));
        lemmaCountTextField.setValue(new DecimalFormat("#,###").format(lemmaCount));
        indexCountTextField.setValue(new DecimalFormat("#,###").format(indexCount));
    }

    private ComboBox<Site> getSiteComboBox() {
        siteComboBox = new ComboBox<>("Сайт:");
        siteComboBox.setItemLabelGenerator(Site::getUrl);

//        siteComboBox.setItems(query -> {
//            return beanAccess.getSiteRepository().getSitesFromPageTable(
//                    PageRequest.of(query.getPage(), query.getPageSize())
//            ).stream();
//        });


        List<Site> siteList = beanAccess.getSiteRepository().getSitesFromPageTable();


        allSiteObject.setId(0);
        allSiteObject.setName("*");
        allSiteObject.setUrl("Все сайты");
        siteList.add(0, allSiteObject);

        siteComboBox.setItems(siteList);

        siteComboBox.addValueChangeListener(event -> {

            clearGrids();
            indexHashMap.clear();

            detailLayout.setVisible(false);

            switch (event.getValue().getName()) {
                case "*" -> {

                    TimeMeasure.setStartTime();
                    //Внимание!!! - долгий запрос, с записью значений для каждого сайта
                    //setInfoValuesFromGetStatistic();

                    //Установка значений путём подсчёта repository.count()
                    //setInfoValuesFromRepositoryCount();

                    //Установк значений по счётчикам удалений
                    setInfoFromCounters();

                    UIElement.showMessage("Время запроса: " + TimeMeasure.getStringExperienceTime());

                }
                default -> {

                    int siteId = event.getValue().getId();
/*
                    Integer pageCount = beanAccess.getPageRepository().countBySiteId(siteId);
                    Integer lemmaCount = beanAccess.getLemmaRepository().countBySiteId(siteId);
                    Integer indexCount = beanAccess.getIndexRepository().countBySiteId(siteId);
*/
                    Site site = event.getValue();
                    Integer pageCount = site.getPageCount();
                    Integer lemmaCount = site.getLemmaCount();
                    Integer indexCount = site.getIndexCount();

                    setInfoValues(pageCount, lemmaCount, indexCount);
                }
            }
            requestLayout.setEnabled(true);
        });
        siteComboBox.setWidth("30%");
        return siteComboBox;
    }

    private void createColumnsFindPageGrid(TextField relevanceTextField,
                                           TextField pathTextField,
                                           TextField titleTextField) {

        findPageGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        findPageGrid.addThemeVariants(GridVariant.LUMO_COMPACT);

        DecimalFormat decimalFormat = new DecimalFormat("#,###.##");


        Grid.Column<PathTable> absRelevanceColumn = findPageGrid.addColumn(new NumberRenderer<>(PathTable::getAbsRelevance,
                        decimalFormat))
                .setHeader("Абсолютная")
                .setTextAlign(ColumnTextAlign.CENTER)
                .setFrozen(true);

        Grid.Column<PathTable> relRelevanceColumn = findPageGrid.addColumn(new NumberRenderer<>(PathTable::getRelRelevance,
                        decimalFormat))
                .setHeader("Относительная")
                .setTextAlign(ColumnTextAlign.CENTER)
                .setFrozen(true);

        Grid.Column<PathTable> pathRelevanceColumn = findPageGrid.addColumn(PathTable::getPath)
                .setHeader("Страница")
                .setTextAlign(ColumnTextAlign.START)
                .setWidth("60%")
                .setAutoWidth(true)
                .setResizable(true);

        HeaderRow headerRow = findPageGrid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText("Релевантность");
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(absRelevanceColumn, relRelevanceColumn).setComponent(simpleCell);

        //Без detail
        //relevanceGrid.setItemDetailsRenderer(createPageDetailRenderer());

        findPageGrid.addItemDoubleClickListener(event -> {
            String path = event.getItem().getPath();
            StartBrowser.startBrowser(path);
        });

        findPageGrid.addSelectionListener(selectionEvent -> {

            selectionEvent.getFirstSelectedItem().ifPresent(t -> {

                detailLayout.setVisible(true);

                relevanceTextField.setValue(t.getRelRelevance().toString());
                pathTextField.setValue(t.getPath());

                Optional<Page> page = beanAccess.getPageRepository().findById(t.getPageId());
                page.ifPresent(p -> {

//                    Document document = Jsoup.parseBodyFragment(p.getContent());
//                    titleTextField.setValue(document.title());
                    String content = p.getContent();
                    int start = content.indexOf("<title>") + 7;
                    int end = content.indexOf("</title>");
                    if (start > 0)
                        titleTextField.setValue(content.substring(start, end));
                    else
                        titleTextField.setValue("");

                    UIElement.removeComponentById(detailLayout, "snippetGrid");

                    Grid<String> grid = UIElement.getStringGrid("Строки контента с найденными леммами:",
                            HtmlParsing.getStringsContainsLemma(content, lemmaGrid.getSelectedItems(), lemmatizator));
                    grid.setId("snippetGrid");
                    detailLayout.add(grid);
                    detailLayout.setVisible(true);
                });
            });
        });
    }

    private void printFindingPageCount(Integer value, String timeString) {
        switch (value) {
            case 0 -> {
                findTextField.setValue("Страницы не найдены");
            }
            case -1 -> findTextField.setValue("Идёт поиск страниц ...");
            case -2 -> findTextField.setValue("");
            default -> {
                String result = "Найдено страниц: " + value;
                if (!timeString.isBlank())
                    result += "   [ -- " + timeString + " -- ]";
                findTextField.setValue(result);
            }
        }
    }

    private void getSearchResults(Integer siteId, Set<Lemma> selectedLemmas, String stringPages) {
        List<PathTable> pathTableList;
        if (siteId == 0) { // Все сайты -> через генерацию запроса - работает меньше минуты при 378 млн. индексов
            String stringLemmas = selectedLemmas.stream()
                    .map(Lemma::getLemma)
                    .collect(Collectors.joining(",", "'", "'"));
            pathTableList = beanAccess.getPathTableRepository().getResultByLemmasAndSiteId(stringLemmas, stringPages, siteId);
        } else { //Выбранный сайт
            String lemmasId = selectedLemmas.stream()
                    .map(l -> l.getId().toString())
                    .collect(Collectors.joining(",", "'", "'"));
            pathTableList = beanAccess.getPathTableRepository().getResult_Function_GetPage(lemmasId, stringPages, siteId);
        }

        findPageGrid.setItems(pathTableList);
        findPageGrid.getColumns().get(2).setHeader("Страниц: " + pathTableList.size());

    }

    private void doLemmaSelectEventIndexHashMap(Set<Lemma> selectedLemmas) {
        setStartTime();
        List<PathTable> pathTableList = findIndexIntersection(selectedLemmas, siteComboBox.getValue().getId());

        //Получить path из базы
//        for (PathTable pathRecord : pathTableList) {
//            beanAccess.getPageRepository().findById(pathRecord.getPageId())
//                    .ifPresent(page-> pathRecord.setPath(page.getPath()));
//        }


        //Получаем list<PathTable> с заполненным Path
        List<PathTable> listPaths = beanAccess.getPathTableRepository().getPaths(pathTableList.stream().map(l -> Integer.toString(l.getPageId())).collect(Collectors.joining(",")));

        BiFunction<PathTable,PathTable,PathTable> ADD_PATH_FUNCTION =
                (p1,p2) -> new PathTable(p1.getPageId(),p1.getAbsRelevance(),p1.getRelRelevance(),p2.getPath());

        HashMap<Integer, PathTable> resultMap = new HashMap<>();
        pathTableList.stream().map(l -> new AbstractMap.SimpleEntry<Integer, PathTable>(l.getPageId(), l))
                .forEach(m -> resultMap.put(m.getKey(), m.getValue()));



        HashMap<Integer, PathTable> pathsMap = new HashMap<>();
        listPaths.stream().map(l -> new AbstractMap.SimpleEntry<Integer, PathTable>(l.getPageId(), l))
                .forEach(m -> pathsMap.put(m.getKey(), m.getValue()));

        //resultMap.forEach((k,v) -> pathsMap.merge(k,v,ADD_PATH_FUNCTION));
        pathsMap.forEach((k,v) -> resultMap.merge(k,v,ADD_PATH_FUNCTION));


        //findPageGrid.setItems(pathTableList);
        findPageGrid.setItems(resultMap.values().stream().sorted(Comparator.comparing(PathTable::getAbsRelevance).reversed()).toList());
        findPageGrid.getColumns().get(2).setHeader("Страниц: " + pathTableList.size());

        printFindingPageCount(pathTableList.size(), TimeMeasure.getStringExperienceTime());
    }

    private void doLemmaSelectEvent(Set<Lemma> selectedLemmas) {

        if (selectedLemmas.size() == 0) {
            printFindingPageCount(0, "");
            return;
        }

        detailLayout.setVisible(false);

        Integer siteId = siteComboBox.getValue().getId();

        TimeMeasure.setStartTime();//----------------------------------------------------------------------------------
        findingPages = findPageIntersection(selectedLemmas, siteId);

        //Retain all pageId - выбираем пересечение страниц для всех лемм
        var pageIdRetained = SearchService.retainAllPageId(lemmaGrid.getSelectedItems(), pageIdHashMap);
        UIElement.showMessage("Найдено " + pageIdRetained.size() + " страниц");
        printFindingPageCount(pageIdRetained.size(), TimeMeasure.getStringExperienceTime());

        if (checkboxAuto.getValue()) {
            getSearchResults(siteId, selectedLemmas, findingPages);
        } else
            findPageGrid.setItems(new ArrayList<>());
    }

    private void doLemmaSelectEventFirst(Set<Lemma> selectedLemmas) {
        if (selectedLemmas.size() == 0) {
            printFindingPageCount(0, "");
            return;
        }

        detailLayout.setVisible(false);
        checkBoxAndButtonLayout.setEnabled(true);

        Integer siteId = siteComboBox.getValue().getId();

        //Формирование списков страниц
        setStartTime();//------------------------------------------------------------------------------------
        selectedLemmas.forEach(lemma -> {
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

        if (selectedLemmas.size() == 0) {
            findPageGrid.setItems(new ArrayList<>());
            findPageGrid.getColumns().get(2).setHeader("Страницы");
            return;
        }
        //Формируем строку со всеми выбранными леммами
        String includeLemma = selectedLemmas.stream()
                .map(Lemma::getLemma)
                .collect(Collectors.joining(",", "'", "'"));

        //Retain all pageId - выбираем пересечение страниц для всех лемм
        var pageIdRetained = SearchService.retainAllPageId(lemmaGrid.getSelectedItems(), pageIdHashMap);
        //----------------------------------------------------------------------------------------------------
        printFindingPageCount(pageIdRetained.size(), TimeMeasure.getStringExperienceTime());

        String includePageId = pageIdRetained.stream().map(p -> Integer.toString(p)).collect(Collectors.joining(",", "'", "'"));

        //if (checkboxAuto.getValue()) ......

        List<PathTable> pathTableList;
        if (siteId == 0)
            pathTableList = beanAccess.getPathTableRepository()
                    .getResultTableForAllSites(includeLemma);
            //.allSitesRequest(includeLemma); неправильно
        else
            pathTableList = beanAccess.getPathTableRepository()
                    .getResultTableForSelectedSite(siteId, includeLemma, includePageId.replace("'", ""));

        //Результаты
        findPageGrid.setItems(pathTableList);
        findPageGrid.getColumns().get(2).setHeader("Страниц: " + pathTableList.size());
    }

    private String findPageIntersection(Set<Lemma> lemmas, int siteId) {

        for (Lemma lemma : lemmas) {
            String selectedLemma = lemma.getLemma();
            List<Integer> pageIdList = null;

            if (!pageIdHashMap.containsKey(selectedLemma)) {

                if (siteId == 0) //Для всех сайтов
                    pageIdList = beanAccess.getPageRepository().getPageIdByLemma(selectedLemma);
                else //Для выбранного сайта
                    pageIdList = beanAccess.getPageRepository().getPageIdBySiteIdAndLemma(selectedLemma, siteId);

                pageIdHashMap.put(selectedLemma, pageIdList); //Для каждой леммы - свой список страниц (pageId)
            }
        }

        //Retain all pageId - выбираем пересечение страниц для всех лемм
        var pageIdRetained = SearchService.retainAllPageId(lemmaGrid.getSelectedItems(), pageIdHashMap);

        return pageIdRetained.stream().map(Object::toString).collect(Collectors.joining(",", "'", "'"));
    }


    private Integer getSiteIdFromLemmas(int searchLemmaId, Set<Lemma> lemmas) {
        for (Lemma lemma : lemmas) {
            if (lemma.getId() == searchLemmaId) return lemma.getSiteId();
        }
        return -1;
    }

    private List<PathTable> getPathTableList(List<IndexEntity> entities, Set<Lemma> lemmas, int selectedSiteId) {

        List<PathTable> result = new ArrayList<>();

        int pageId = 0;
        float abs = 0;
        float rel = 0;
        float max_rank = 0;

        for (IndexEntity indexEntity : entities) {

            int nextPage = indexEntity.getPageId();

            if ((nextPage != pageId) && (pageId != 0)) {
                rel = abs / max_rank;
                //get path
                int lemmaSiteId = getSiteIdFromLemmas(indexEntity.getLemmaId(), lemmas);

                //Добавляем в результат
                if ((lemmaSiteId == selectedSiteId) || (selectedSiteId == 0))
                    result.add(new PathTable(pageId, abs, rel, Integer.toString(pageId)));
                max_rank = 0;
                abs = 0;
                rel = 0;
            }
            pageId = indexEntity.getPageId();
            abs += indexEntity.getRank();
            if (max_rank < indexEntity.getRank()) max_rank = indexEntity.getRank();
        }
        if (abs != 0) { //Добавляем последнюю - если она есть
            result.add(new PathTable(pageId, abs, rel, Integer.toString(pageId)));
        }
        result.sort(Comparator.comparing(PathTable::getAbsRelevance).reversed());
        return result;
    }

    private List<PathTable> findIndexIntersection(Set<Lemma> lemmas, int siteId) {

        if (lemmas.size() == 0) return new ArrayList<>();

        List<IndexEntity> listIndex;
        //List<PathTable> result = new ArrayList<>();

        for (Lemma lemma : lemmas) {
            String selectedLemma = lemma.getLemma();
            if (!indexHashMap.containsKey(selectedLemma)) {
                if (siteId == 0)
                    listIndex = beanAccess.getIndexRepository().getIndexByLemmaForAllSites(selectedLemma);
                else
                    listIndex = beanAccess.getIndexRepository().getIndexByLemmaForSiteId(selectedLemma, siteId);

                indexHashMap.put(selectedLemma, listIndex);
            }
        }
        //PageId
        HashMap<String, List<Integer>> pageIdHashMap = new HashMap<>();

        //Взять только выбранные леммы
        for (Lemma lemma : lemmas) {
            pageIdHashMap.put(lemma.getLemma(),
                    indexHashMap.get(lemma.getLemma()).stream().map(IndexEntity::getPageId).toList());
        }
        //Найти пересечение
        List<Integer> joinPageId = SearchService.retainAllPageId(lemmaGrid.getSelectedItems(), pageIdHashMap);

        List<IndexEntity> joinedIndexEntities = SearchService.removeByPageId(indexHashMap, joinPageId);

        return getPathTableList(joinedIndexEntities, lemmas, siteId);
    }

    private void createColumnsLemmaGrid() {
        lemmaGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        UIElement.setAllCheckboxVisibility(lemmaGrid, false);
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

            //Стираем результаты
            if (selectionEvent.getAllSelectedItems().size() == 0)
                findPageGrid.setItems(new ArrayList<>());


            switch (selectGetInfoQueryComboBox.getValue()) {
                case "first variant" -> doLemmaSelectEventFirst(selectionEvent.getAllSelectedItems());
                case "second variant" -> doLemmaSelectEvent(selectionEvent.getAllSelectedItems());
                default -> doLemmaSelectEventIndexHashMap(selectionEvent.getAllSelectedItems());
            }


            //checkBoxAndButtonLayout.setEnabled(true);
            detailLayout.setVisible(false);
        });

        calcPageButton.addClickListener(buttonClickEvent -> {

            if (Objects.equals(selectGetInfoQueryComboBox.getValue(), "first variant")) {
                //doLemmaSelectEventFirst(selectionEvent.getAllSelectedItems());
            } else
                getSearchResults(siteComboBox.getValue().getId(), lemmaGrid.getSelectedItems(), findingPages);

        });

    }


    private Button getSearchButton() {

        var searchButton = UIElement.createButton("Найти леммы", VaadinIcon.SEARCH_PLUS, "");
        searchButton.setWidth("20%");

        searchButton.addClickListener(buttonClickEvent -> {
            String requestStr = requestTextField.getValue();

            if (requestStr.isBlank()) {
                UIElement.showMessage("Запрос не может быть пустым");
                return;
            } else if (HtmlParsing.getRussianListString(requestStr).size() == 0) {
                UIElement.showMessage("Запрос должен содержать русские слова!");
                return;
            }
            //Отображаем леммы
            setLemmaGridItems(siteComboBox.getValue().getId());
        });
        return searchButton;
    }

    private void setLemmaGridItems(int siteId) {
        //Все лемммы из запроса
        HashMap<String, Integer> requestLemmas = lemmatizator.getLemmaHashMap(requestTextField.getValue());
        if (requestLemmas.size() == 0) {
            UIElement.showMessage("Леммы не найдены. Измените запрос.");
            clearGrids();
            return;
        }
        String includeLemma = requestLemmas.keySet().stream().collect(Collectors.joining("','", "'", "'"));
        //lemmaGrid.setItems(beanAccess.getPathTableRepository().findLemmasInAllSites(includeLemma));

        if (siteId == 0) { //Все сайты
            //Запрос в программе выдаёт неверный результат, тот же запрос в pgAdmin работает правильно
            //lemmaGrid.setItems(beanAccess.getLemmaRepository().findByLemmaIn(lemmaList));
            //Причина неизвестна - БАГ!!!

            //Реализовал через jdbcTemplate
            lemmaGrid.setItems(beanAccess.getPathTableRepository().findLemmasInAllSites(includeLemma));
        } else  //Выбранный сайт
            lemmaGrid.setItems(query -> beanAccess.getLemmaRepository()
                    .findBySiteIdAndLemmaIn(
                            siteId,
                            requestLemmas.keySet().stream().toList(),
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

    private Button createBrowserButton(Grid<PathTable> grid) {

        var button = UIElement.createButton("", VaadinIcon.BROWSER, "Открыть в браузере");
        button.addClickListener(event -> {
            grid.getSelectedItems().stream().findFirst()
                    .ifPresent(pathLine -> StartBrowser.startBrowser(pathLine.getPath()));
        });
        return button;
    }

    private VerticalLayout getDetailLayout() {

        var relevanceTextField = new TextField("Релевантность");
        var pathTextField = new TextField("Адрес страницы");
        var titleTextField = new TextField("title");

        relevanceTextField.setWidth("15%");
        pathTextField.setWidth("85%");
        titleTextField.setWidth("100%");

        relevanceTextField.setReadOnly(true);
        pathTextField.setReadOnly(true);
        titleTextField.setReadOnly(true);

        var horizontalLayout = new HorizontalLayout(relevanceTextField,
                pathTextField, createBrowserButton(findPageGrid));
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        horizontalLayout.setWidth("100%");

        detailLayout.add(horizontalLayout, titleTextField);
        detailLayout.setWidthFull();

        createColumnsFindPageGrid(relevanceTextField, pathTextField, titleTextField);

        return detailLayout;
    }

}
