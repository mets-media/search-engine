package engine.views;


import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import engine.entity.Field;
import engine.entity.Page;
import engine.entity.Site;
import engine.repository.FieldRepository;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IndexingComponent {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private FieldRepository fieldRepository;
    private EntityManager entityManager;
    private VerticalLayout mainLayout;
    private Grid<Field> fieldGrid = new Grid<>(Field.class, false);
    private Grid<Page> grid = null;

    private HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();

    public IndexingComponent() {
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Настройки индексации", null));

        createTabs(List.of("Страницы сайта", "HTML Блоки"));


    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
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
            VerticalLayout content = null;
            switch (label) {
                case "Страницы сайта" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createPageComponent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }

                }
                case "HTML Блоки" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createHTMLBlocksContent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }
                    fieldGrid.setItems(fieldRepository.findAll());
                }
            }
            contentsHashMap.get(label).setVisible(true);
        });

        //contentsHashMap.put("Страницы сайта", createPageComponent());
        //mainLayout.add(contentsHashMap.get("Страницы сайта"));

    }

    @PostConstruct
    private VerticalLayout createHTMLBlocksContent() {
        fieldGrid.addColumn(Field::getName).setHeader("Наименование").setTextAlign(ColumnTextAlign.START).setSortable(true);
        fieldGrid.addColumn(Field::getSelector).setHeader("Селектор").setTextAlign(ColumnTextAlign.CENTER).setSortable(true);
        fieldGrid.addColumn(Field::getWeight).setHeader("Коэффициент").setTextAlign(ColumnTextAlign.CENTER).setSortable(true);

        fieldGrid.setItems(fieldRepository.findAll());
        return new VerticalLayout(fieldGrid);
    }

    public void dataAccess(FieldRepository fieldRepository,
                           PageRepository pageRepository,
                           SiteRepository siteRepository,
                           EntityManager entityManager) {
        this.fieldRepository = fieldRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.entityManager = entityManager;
    }


    private VerticalLayout createPageComponent() {

        ComboBox<String> siteComboBox = new ComboBox<>("страницы сайта:");

//        List<String> siteList = new ArrayList<>();
//        for (Site site : siteRepository.findSitesFromPageTable()) {
//            siteList.add(site.getUrl());
//        }
//        siteComboBox.setItems(siteList);


        siteComboBox.setItems(query -> {
            return siteRepository.getSitesUrlFromPageTable(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });


        siteComboBox.addValueChangeListener(event -> { //==============================================================
            siteRepository.getSiteByUrl(event.getValue()).ifPresent(site -> {
                //------------------------------------------------------------------------------------------
//                List pages = entityManager.createQuery("from Page Where Site_Id = :siteId order by Path")
//                                        .setParameter("siteId", site.getId())
//                        .setMaxResults(10)
//                        .getResultList();
//                pageGrid.setItems(pages);
                //------------------------------------------------------------------------------------------

                grid.setItems(query -> pageRepository
                        .findBySiteId(
                                site.getId(),
                                PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("path")))
                        .stream());
            });
        });//===========================================================================================================



        if (grid == null) {
            grid = new Grid<>(Page.class, false);
            grid.addColumn(Page::getCode)
                    .setHeader("Code")
                    .setSortable(true)
                    .setAutoWidth(true);
            grid.addColumn(Page::getPath)
                    .setHeader("Path")
                    .setKey("path")
                    .setSortable(true)
                    .setResizable(true);
        }
        return new VerticalLayout(siteComboBox, grid);


//        DataProvider<Page, Void> dataProvider = DataProvider.fromCallbacks(
//                query -> {
//                        int offset = query.getOffset();
//                        int limit = query.getLimit();
//
//                        OffsetRequest request = new OffsetRequest();
//                        request.setLimit(limit);
//                        request.setOffset(offset);
//
//                        return pageRepository.findAll(request, sort);
//                }
//
//
//        );


//        VerticalLayout leftVLayout = new VerticalLayout(pageGrid);
//        leftVLayout.setSizeFull();
//
//        TextField textField = new TextField("Результат");
//        textField.setSizeFull();
//        VerticalLayout rightVLayout = new VerticalLayout(textField);
//
//
//        return new VerticalLayout();
    }

}
