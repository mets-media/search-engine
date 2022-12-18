package engine.views;

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
import engine.repository.*;
import engine.service.BeanAccess;
import engine.service.HtmlParsing;
import engine.service.Lemmatization;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class SearchComponent {

    private static BeanAccess beanAccess;
    private ComboBox<Site> siteComboBox = null;
    private static final HashMap<String, List<String>> pagesHashMap = new HashMap<>();
    private static final HashMap<String, List<Integer>> pageIdHashMap = new HashMap<>();

//    private static SiteRepository siteRepository;
//    private static PageRepository pageRepository;
//    private static LemmaRepository lemmaRepository;
//    private static PathTableRepository pathTableRepository;
//    private static PartOfSpeechRepository partOfSpeechRepository;


    private final VerticalLayout mainLayout;
    private final HorizontalLayout requestLayout = new HorizontalLayout();
    private final HorizontalLayout gridsLayout = new HorizontalLayout();
    private final Grid<Lemma> lemmaGrid = new Grid<>(Lemma.class, false);

    // private final Grid<String> pageGrid = new Grid<>(String.class, false);
    private final Grid<PathTable> relevanceGrid = new Grid<>(PathTable.class, false);
    private final TextField pageCountTextField = new TextField("Количество страниц");
    private final TextField lemmaCountTextField = new TextField("Количество лемм");
    private final TextField requestTextField = new TextField("Поисковый запрос");

    private static final TextArea htmlTextArea = new TextArea("Snippet: <b> tag");


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

//            PageRepository pRepository,
//                                     SiteRepository sRepository,
//                                     LemmaRepository lRepository,
//                                     PartOfSpeechRepository posRepository,
//                                     PathTableRepository ptRepository) {
//        pageRepository = pRepository;
//        siteRepository = sRepository;
//        lemmaRepository = lRepository;
//        partOfSpeechRepository = posRepository;
//        pathTableRepository = ptRepository;
//    }

    private VerticalLayout createSearchComponent() {

        pageCountTextField.setReadOnly(true);
        lemmaCountTextField.setReadOnly(true);
        var horizontalLayout = new HorizontalLayout(
                createSiteComboBox(),
                pageCountTextField,
                lemmaCountTextField);
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

        siteComboBox.setItems(query -> {
            return beanAccess.getSiteRepository().getSitesFromPageTable(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });

        siteComboBox.addValueChangeListener(event -> {
            requestLayout.setEnabled(true);
            int siteId = event.getValue().getId();
            Integer pageCount = beanAccess.getPageRepository().countBySiteId(siteId);
            Integer lemmaCount = beanAccess.getLemmaRepository().countBySiteId(siteId);
            pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));
            lemmaCountTextField.setValue(new DecimalFormat("#,###").format(lemmaCount));
        });
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
//        headerRow.join(absRelevanceColumn, relRelevanceColumn).setText("Релевантность");

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

//    private void createColumnsPageGrid() {
//        pageGrid.addColumn(String::toString).setHeader("Страницы");
//
//        pageGrid.addItemDoubleClickListener(event -> {
//            String path = event.getItem();
//            StartBrowser.startBrowser(path);
//        });
//    }

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

                List<Integer> pageIdList = beanAccess.getPageRepository().getPageIdBySiteIdLemmaIn(selectedLemma, siteId);
                if (!pageIdHashMap.containsKey(selectedLemma))
                    pageIdHashMap.put(selectedLemma, pageIdList);
            });

            StringBuilder stringBuilder = new StringBuilder();
            selectionEvent.getAllSelectedItems().forEach(l -> stringBuilder.append(l.getLemma()).append(","));
            String includeLemma = stringBuilder.toString();


            if (includeLemma.isEmpty()) {
                relevanceGrid.setItems(new ArrayList<>());
                relevanceGrid.getColumns().get(2).setHeader("Страницы");
                return;
            }
            includeLemma = "'" + includeLemma.substring(0, includeLemma.length() - 1) + "'";

            var pageIdRetained = retainAllPageId(pageIdHashMap);

            stringBuilder.delete(0, stringBuilder.length());

            pageIdRetained.forEach(pageId -> stringBuilder.append(pageId.toString()).append(","));

            var includePageId = stringBuilder.toString();

            if (!(includePageId.isBlank())) {
                includePageId = includePageId.substring(0, includePageId.length() - 1);

                List<PathTable> pathTableList = beanAccess.getPathTableRepository()
                        .getResultTable(siteId, includeLemma, includePageId);

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

            List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());

            Lemmatization lemmatizator = new Lemmatization(excludeList, null);

            HashMap<String, Integer> requestLemmas = lemmatizator.getLemmaCount(requestTextField.getValue());

            Integer siteId = siteComboBox.getValue().getId();
            List<String> lemmaList = requestLemmas.keySet().stream().toList();

            lemmaGrid.setItems(query -> beanAccess.getLemmaRepository()
                    .findBySiteIdAndLemmaIn(
                            siteId,
                            lemmaList,
                            PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("frequency")))
                    .stream());

        });
        return searchButton;
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
