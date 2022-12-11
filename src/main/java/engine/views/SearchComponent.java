package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import engine.entity.Lemma;
import engine.entity.Site;
import engine.repository.*;
import engine.service.Lemmatization;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static engine.views.CreateUI.showMessage;

public class SearchComponent {
    private Site site = null;

    private static HashMap<String, List<String>> pagesHashMap = new HashMap<>();
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static LemmaRepository lemmaRepository;
    private static IndexRepository indexRepository;
    private static PartOfSpeechRepository partOfSpeechRepository;
    private final VerticalLayout mainLayout;
    private final HorizontalLayout requestLayout = new HorizontalLayout();
    private final HorizontalLayout gridsLayout = new HorizontalLayout();
    private final Grid<Lemma> lemmaGrid = new Grid<>(Lemma.class, false);
    //private final Grid<Page> pageGrid = new Grid<>(Page.class, false);
    private final Grid<String> pageGrid = new Grid<>(String.class, false);
    private final TextField pageCountTextField = new TextField("Количество страниц");
    private final TextField lemmaCountTextField = new TextField("Количество лемм");
    private final TextField requestTextField = new TextField("Поисковый запрос");

    public SearchComponent() {
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Система поиска", "xl", null));
        mainLayout.add(createSearchComponent());
        requestLayout.setSizeFull();
        requestTextField.setSizeFull();
        lemmaGrid.setWidth("40%");
        pageGrid.setWidth("60%");
    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    public static void setDataAccess(PageRepository pRepository,
                                     SiteRepository sRepository,
                                     LemmaRepository lRepository,
                                     IndexRepository iRepository,
                                     PartOfSpeechRepository posRepository) {
        pageRepository = pRepository;
        siteRepository = sRepository;
        lemmaRepository = lRepository;
        indexRepository = iRepository;
        partOfSpeechRepository = posRepository;
    }

    private VerticalLayout createSearchComponent() {
        ComboBox<String> siteComboBox = new ComboBox<>("Сайт:");

        siteComboBox.setItems(query -> {
            return siteRepository.getSitesUrlFromPageTable(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });

        siteComboBox.addValueChangeListener(event -> {

            requestLayout.setEnabled(true);

            siteRepository.getSiteByUrl(event.getValue()).ifPresent(site -> {
                //------------------------------------------------------------------------------------------
//                List pages = entityManager.createQuery("from Page Where Site_Id = :siteId order by Path")
//                                        .setParameter("siteId", site.getId())
//                        .setMaxResults(10)
//                        .getResultList();
//                pageGrid.setItems(pages);
                //------------------------------------------------------------------------------------------

                this.site = site;
                Integer pageCount = pageRepository.countBySiteId(site.getId());
                Integer lemmaCount = lemmaRepository.countBySiteId(site.getId());

                pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));
                lemmaCountTextField.setValue(new DecimalFormat("#,###").format(lemmaCount));
            });
        });

        pageCountTextField.setReadOnly(true);
        lemmaCountTextField.setReadOnly(true);
        var horizontalLayout = new HorizontalLayout(siteComboBox,
                pageCountTextField,
                lemmaCountTextField);
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        horizontalLayout.setSizeUndefined();

        createColumnsLemmaGrid();
        createColumnsPageGrid();

        requestTextField.setSizeUndefined();

        requestLayout.add(requestTextField, createSearchButton());
        requestLayout.setSizeUndefined();
        requestLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        requestLayout.setEnabled(false);

        gridsLayout.add(lemmaGrid, pageGrid);
        gridsLayout.setWidthFull();

        return new VerticalLayout(horizontalLayout, requestLayout, gridsLayout);
    }

    private void createColumnsPageGrid() {
        pageGrid.addColumn(String::toString).setHeader("Страницы");

        pageGrid.addItemDoubleClickListener(event -> {
            String path = event.getItem();
            StartBrowser.startBrowser(path);
        });
    }


    private void createColumnsLemmaGrid() {
        lemmaGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        CreateUI.setAllCheckboxVisibility(lemmaGrid, true);
        lemmaGrid.addColumn(Lemma::getFrequency).setHeader("Частота")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);
        lemmaGrid.addColumn(Lemma::getLemma).setHeader("Лемма")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.START);

        //Действие при выборе леммы
        lemmaGrid.addSelectionListener(selectionEvent -> {

            Integer siteId = site.getId();

            selectionEvent.getAllSelectedItems().forEach(lemma -> {
                String selectedLemma = lemma.getLemma();
                List<String> paths = pageRepository.getPathsBySiteIdLemma(selectedLemma, siteId);
                if (!pagesHashMap.containsKey(selectedLemma))
                    pagesHashMap.put(selectedLemma, paths);
            });

            pageGrid.setItems(retainAllSelectedLemmas());
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

    private Button createSearchButton() {
        Button searchButton = new Button("Найти страницы");

        searchButton.addClickListener(buttonClickEvent -> {

            List<String> excludeList = partOfSpeechRepository.findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());

            Lemmatization lemmatizator = new Lemmatization(excludeList, null);

            HashMap<String, Integer> requestLemmas = lemmatizator.getLemmaCount(requestTextField.getValue());

            lemmaGrid.setItems(query -> lemmaRepository
                    .findBySiteIdAndLemmaIn(
                            site.getId(),
                            requestLemmas.keySet().stream().toList(),
                            PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("frequency")))
                    .stream());
        });
        return searchButton;
    }
}
