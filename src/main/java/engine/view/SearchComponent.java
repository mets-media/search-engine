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
import java.util.stream.Collectors;

public class SearchComponent {
    private static BeanAccess beanAccess;
    private ComboBox<Site> siteComboBox = null;
    private final HashMap<String, List<Integer>> pageIdHashMap = new HashMap<>();
    private final HashMap<String, List<Index>> indexHashMap = new HashMap<>();
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
    private final Checkbox checkboxAuto = new Checkbox("Автом.режим");
    private final Button calcPageButton = UIElement.createButton("Расчёт релевантности", VaadinIcon.DOWNLOAD, "");
    private final VerticalLayout detailLayout = new VerticalLayout();
    private ComboBox<String> selectCountersQueryComboBox;
    private ComboBox<String> selectGetInfoQueryComboBox;
    private final Site allSiteObject = new Site();
    private final Lemmatization lemmatizator;
    private String resultSQL = "";

    public SearchComponent() {
        mainLayout = UIElement.getMainLayout();
        mainLayout.add(UIElement.getTopLayout("Система поиска", "xl", null));
        mainLayout.add(getSearchComponent());

        requestLayout.setSizeFull();
        requestTextField.setSizeFull();
        requestTextField.setPrefixComponent(VaadinIcon.SEARCH.create());
        requestTextField.setPrefixComponent(VaadinIcon.SEARCH.create());
        requestTextField.setClearButtonVisible(true);

        lemmaGrid.setWidth("40%");
        findPageGrid.setWidth("60%");

        lemmaGrid.setHeight(250, Unit.PIXELS);
        findPageGrid.setHeight(250, Unit.PIXELS);

        detailLayout.setVisible(false);

        Lemmatization.setBeanAccess(beanAccess);
        lemmatizator = Lemmatization.getLemmatizator();
    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    public static void setBeanAccess(BeanAccess beanAccess) {
        SearchComponent.beanAccess = beanAccess;
    }

    private void checkAndSetEnableAutoMode(String selectedSite, String selectedMode) {
        if ((selectedSite.equals("Java HashMap")) ||
                        ((selectedSite.equals("Statement gen.")) && (selectedMode.equals("Все сайты")))) {
            checkboxAuto.setEnabled(false);
            calcPageButton.setEnabled(false);
            findTextField.setEnabled(false);
        } else {
            checkboxAuto.setEnabled(true);
            calcPageButton.setEnabled(true);
            findTextField.setEnabled(true);
        }

        calcPageButton.setEnabled(!checkboxAuto.getValue());
    }

    private VerticalLayout getSearchComponent() {

        Collection<TextField> textFieldCollection = Arrays.asList(pageCountTextField, lemmaCountTextField, indexCountTextField);
        textFieldCollection.forEach(textField -> {
            textField.setReadOnly(true);
            textField.setWidth("15%");
        });

        selectCountersQueryComboBox = UIElement.createComboBox(List.of("GetStatistic", "Repository.Count", "Counters"));
        selectCountersQueryComboBox.setVisible(false);
        selectGetInfoQueryComboBox = UIElement.createComboBox(List.of("PostgreSQL", "Java HashMap", "Statement gen."));
        selectGetInfoQueryComboBox.addValueChangeListener(event -> {
            resultSQL = "Empty";
            checkAndSetEnableAutoMode(event.getValue(), siteComboBox.getValue().getUrl());
        });

        //--------------      Сайт Страницы Леммы Index обновить --------------
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
        requestLayout.add(requestTextField, getSearchLemmaButton());
        requestLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        requestLayout.setEnabled(false);

        //-------------- Grid Леммы  --- Grid Страницы ----------------------
        gridsLayout.add(lemmaGrid, findPageGrid);
        gridsLayout.setWidthFull();


        checkboxAuto.setValue(true);
        checkboxAuto.addValueChangeListener(event -> calcPageButton.setEnabled(!event.getValue()));
        findTextField.setWidth("50%");
        findTextField.setReadOnly(true);

        calcPageButton.setEnabled(false);
        //---------------- Выбор системы поиска, Режим Авто -------------------------
        var hLayout = new HorizontalLayout(selectGetInfoQueryComboBox, checkboxAuto);
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        hLayout.setWidth("40%");

        checkBoxAndButtonLayout.setSizeUndefined();
        checkBoxAndButtonLayout.add(hLayout, calcPageButton, findTextField);
        checkBoxAndButtonLayout.setAlignItems(FlexComponent.Alignment.END);
        checkBoxAndButtonLayout.setWidthFull();
        checkBoxAndButtonLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

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
            siteComboBox.setValue(allSiteObject);
            UIElement.showMessage("Время подсчёта всех страниц, лемм и индексов: " + TimeMeasure.getStringExperienceTime());

        });
        return button;
    }

    private void clearGrids() {
        lemmaGrid.setItems(new ArrayList<>());
        findPageGrid.setItems(new ArrayList<>());
        pageIdHashMap.clear();
    }

    public void setInfoValuesFromGetStatistic() {
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
        Optional<Site> allSite = beanAccess.getSiteRepository().getAllSiteCountersInfo();
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

        List<Site> siteList = beanAccess.getSiteRepository().getSitesFromPageTable();

        allSiteObject.setId(0);
        allSiteObject.setName("*");
        allSiteObject.setUrl("Все сайты");
        siteList.add(0, allSiteObject);

        siteComboBox.setItems(siteList);

        siteComboBox.addValueChangeListener(event -> {

            checkAndSetEnableAutoMode(selectGetInfoQueryComboBox.getValue(), event.getValue().getUrl());

            clearGrids();
            indexHashMap.clear();

            detailLayout.setVisible(false);

            switch (event.getValue().getName()) {
                case "*" -> setInfoFromCounters();
                default -> {
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

        Grid.Column<PathTable> pathColumn = findPageGrid.addColumn(PathTable::getPath)
                .setHeader("Страница")
                .setTextAlign(ColumnTextAlign.START)
                .setWidth("60%")
                .setAutoWidth(true)
                .setResizable(true);

        Grid.Column<PathTable> pageIdColumn = findPageGrid.addColumn(PathTable::getPageId)
                .setHeader("Страница")
                .setTextAlign(ColumnTextAlign.START)
                .setFrozen(true);
        pageIdColumn.setVisible(false);

        HeaderRow headerRow = findPageGrid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText("Релевантность");
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(absRelevanceColumn, relRelevanceColumn).setComponent(simpleCell);

        Div simpleCell_ = new Div();
        simpleCell_.setText("");
        simpleCell_.getElement().getStyle().set("text-align", "center");
        headerRow.join(pathColumn, pageIdColumn).setComponent(simpleCell_);

        findPageGrid.addItemDoubleClickListener(event -> {
            String path = event.getItem().getPath();
            StartBrowser.startBrowser(path);
        });

        findPageGrid.addSelectionListener(selectionEvent -> {

            findTextField.setValue("");

            selectionEvent.getFirstSelectedItem().ifPresent(t -> {

                detailLayout.setVisible(true);

                relevanceTextField.setValue(t.getRelRelevance().toString());
                pathTextField.setValue(t.getPath());

                Optional<Page> page = beanAccess.getPageRepository().findById(t.getPageId());
                page.ifPresent(p -> {
                    String content = p.getContent();
                    int start = content.indexOf("<title>") + 7;
                    int end = content.indexOf("</title>");
                    if (start > 0)
                        titleTextField.setValue(content.substring(start, end));
                    else
                        titleTextField.setValue("");

                    UIElement.removeComponentById(detailLayout, "snippetGrid");

                    Grid<String> grid = UIElement.getStringGrid("Строки контента с найденными леммами:",
                            HtmlParsing.getHTMLStringsContainsLemma(content, lemmaGrid.getSelectedItems(), lemmatizator));
                    grid.setId("snippetGrid");

                    UIElement.removeComponentById(detailLayout, "indexGrid");

                    detailLayout.add(grid);
                    detailLayout.setVisible(true);
                });
            });
        });
    }

    private void printFindingPageCount(Integer value, String timeString) {

        String result;
        switch (value) {
            case 0 -> result = "Страницы не найдены";
            case -1 -> result = "Идёт поиск страниц ...";
            case -2 -> result = "";
            case -3 -> result = "Время поиска " + "[ -- " + timeString + " -- ]";
            default -> {
                result = "Найдено страниц: " + value;
                if (!timeString.isBlank()) {
                    result += "   [ -- " + timeString + " -- ]";
                }
            }
        }
        findPageGrid.getColumns().get(2).setHeader(result);
    }

    private List<PathTable> getSearchResults() {
        List<PathTable> pathTableList = beanAccess.getImplRepository().findPathTableItems(resultSQL);

        findPageGrid.setItems(pathTableList);
        printFindingPageCount(pathTableList.size(), TimeMeasure.getStringExperienceTime());
        return pathTableList;
    }

    private List<PathTable> doLemmaSelectEvent_JavaHashMap(Set<Lemma> selectedLemmas, int siteId) {

        resultSQL = "Empty";

        /**
         *  - Результаты отдельных запросов по каждой лемме -> HashMap<Лемма, List<IndexEntity>
         *  - Находим пересечениие всех List<IndexEntity>
         *  - Подгружаем Path из таблицы Page для найденного множества IndexEntity
         */

        printFindingPageCount(-1, "");
        List<PathTable> pathTableList =
                SearchService.findIndexIntersection(selectedLemmas, indexHashMap, siteId);

        if (pathTableList.size() != 0) {
            //Получаем list<PathTable> с заполненным Path
            String pageIdArray = pathTableList.stream()
                    .map(l -> Integer.toString(l.getPageId()))
                    .collect(Collectors.joining(","));
            List<PathTable> listPaths = beanAccess.getImplRepository()
                    .findPathTableItems(SearchService.getSQLByName("getPaths")
                            .replace(":pageIdArray", pageIdArray));
            return SearchService.listMergeEx(pathTableList, listPaths);
        }
        return new ArrayList<>();
    }

    private List<PathTable> doLemmaSelectEvent(Set<Lemma> selectedLemmas, int siteId) {
        detailLayout.setVisible(false);

        List<PathTable> pathTableList;
        if (siteId == 0) { // Все сайты
            resultSQL = SearchService.getGeneratedSQL(selectedLemmas);
        } else { //Выбранный сайт
            String pageIdArray;
            String lemmaIdArray;
            //----------------------------------------------------------------------------------
            List<Integer> listPagesId = SearchService.getCommonPages(selectedLemmas, siteId, pageIdHashMap);

            /** Формируем строку с перечислением Page_Id **/
            pageIdArray = listPagesId.stream().map(p -> Integer.toString(p))
                    .collect(Collectors.joining(","));

            /** Формируем строку с перечислением Lemma_Id **/
            lemmaIdArray = SearchService.getLemmaIdString(selectedLemmas, siteId);
            //----------------------------------------------------------------------------------

            if (listPagesId.size() != 0) { //Не смысла запрашивать - если страниц не существует

                resultSQL = SearchService.getSQLByName("oneSiteQuery")
                        .replace(":lemmaIdArray", lemmaIdArray)
                        .replace(":pageIdArray", pageIdArray);
                if (!checkboxAuto.getValue()) {
                    findTextField.setValue("Найдено страниц: " + listPagesId.size());
                    return new ArrayList<>();
                }
            } else {
                resultSQL = "Empty";
                printFindingPageCount(0, TimeMeasure.getStringExperienceTime());
                return new ArrayList<>();
            }
        }
        return beanAccess.getImplRepository().findPathTableItems(resultSQL);
    }

    private List<PathTable> doLemmaSelectEvent_PostgreSQL(Set<Lemma> selectedLemmas, int siteId) {
        detailLayout.setVisible(false);

        String pageIdArray;
        String lemmaIdArray;

        /** Формируем строку с перечислением Page_Id **/
        List<Integer> listPagesId = SearchService.getCommonPages(selectedLemmas, siteId, pageIdHashMap);
        pageIdArray = listPagesId.stream().map(p -> Integer.toString(p))
                .collect(Collectors.joining(","));

        lemmaIdArray = SearchService.getLemmaIdString(selectedLemmas, siteId);

        List<PathTable> pathTableList;
        if (siteId == 0) {
            resultSQL = SearchService.getSQLByName("getResult_INDEX_PAGE_LEMMA")
                    .replace(":lemmaIdArray", lemmaIdArray)
                    .replace(":pageIdArray", pageIdArray)
                    .replace(":siteId", Integer.toString(siteId));

        } else {//-------------------------------------------------------------------------------------------------
            resultSQL = SearchService.getSQLByName("getResult_GetPage_PAGE_INDEX")
                    .replace(":lemmaIdArray", lemmaIdArray)
                    .replace(":pageIdArray", pageIdArray);

        }//--------------------------------------------------------------------------------------------------------

        if (!checkboxAuto.getValue()) {
            findTextField.setValue("Найдено страниц: " + listPagesId.size());
            return new ArrayList<>();
        }

        pathTableList = beanAccess.getImplRepository().findPathTableItems(resultSQL);

        printFindingPageCount(pathTableList.size(), TimeMeasure.getStringExperienceTime());

        return pathTableList;
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

            //Стираем предыдущие результаты
            if (selectionEvent.getAllSelectedItems().size() == 0) {
                findPageGrid.setItems(new ArrayList<>());
                findTextField.setValue("");
                printFindingPageCount(0, "");
                return;
            }

            TimeMeasure.setStartTime();
            List<PathTable> pathTableList = null;

            int siteId = siteComboBox.getValue().getId();

            switch (selectGetInfoQueryComboBox.getValue()) {
                case "PostgreSQL" -> pathTableList =
                        doLemmaSelectEvent_PostgreSQL(selectionEvent.getAllSelectedItems(), siteId);
                case "Statement gen." -> pathTableList =
                        doLemmaSelectEvent(selectionEvent.getAllSelectedItems(), siteId);
                default -> pathTableList =
                        doLemmaSelectEvent_JavaHashMap(selectionEvent.getAllSelectedItems(), siteId);
            }

            findPageGrid.setItems(pathTableList);
            printFindingPageCount(pathTableList.size(), TimeMeasure.getStringExperienceTime());

            detailLayout.setVisible(false);
        });

        calcPageButton.addClickListener(buttonClickEvent -> {
            getSearchResults();
        });

    }

    private Button getSearchLemmaButton() {

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

        if (siteId == 0) { //Все сайты
            lemmaGrid.setItems(beanAccess.getImplRepository().findLemmasInAllSites(includeLemma));
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
