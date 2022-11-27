package engine.views;


import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import engine.entity.Field;
import engine.entity.Page;
import engine.entity.Site;
import engine.repository.FieldRepository;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import org.springframework.data.domain.PageRequest;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.criteria.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class IndexingComponent {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private FieldRepository fieldRepository;
    private EntityManager entityManager;
    private VerticalLayout mainLayout;
    private Grid<Field> fieldGrid = new Grid<>(Field.class, false);
    private Grid<Page> pageGrid = null;
    private Grid<Page> gridVariant = null;

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
        List<String> siteList = new ArrayList<>();


        for (Site site : siteRepository.getSitesFromPageTable()) {
            siteList.add(site.getUrl());
        }

        siteComboBox.setItems(siteList);


        siteComboBox.addValueChangeListener(event -> {
            siteRepository.getSiteByUrl(event.getValue()).ifPresent(site -> {

                //==========================================================================================
                List pages = entityManager.createQuery("from Page Where Site_Id = :siteId order by Path")
                                        .setParameter("siteId", site.getId())
                        .setMaxResults(10)
                        .getResultList();
                pageGrid.setItems(pages);
                //==========================================================================================

//                gridVariant.setItems(query -> (Stream<Page>) pageRepository.findLinksBySiteId(
//                        site.getId(),PageRequest.of(query.getPage(),query.getPageSize())));


            });



        });

        if (gridVariant == null) {
            gridVariant = new Grid<>(Page.class, false);

            //================= Сортировка ==========================
//            gridVariant.addColumn(page -> page.getPath())
//                    .setHeader("Path")
//                    .setKey("path")  //ключь который передаётся в callback
//                    .setSortable(true)
//                    .setResizable(true);
//            gridVariant.setItems(VaadinSpringDataHelpers.fromPagingRepository(pageRepository));
            //=======================================================

            //=======================================================

            //=======================================================






//            gridVariant.setItems(query -> {
//               return pageRepository.findAll(
//                       PageRequest.of(query.getPage(), query.getPageSize())
//               ).stream();
//            });
        }



        if (pageGrid == null) {
            pageGrid = new Grid<>(Page.class, false);
            pageGrid.addColumn(Page::getPath).setSortable(true).setResizable(true);
            pageGrid.addColumn(Page::getCode).setSortable(true).setResizable(true);
        }
        //return new VerticalLayout(siteComboBox, pageGrid);
        return new VerticalLayout(siteComboBox, gridVariant);




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
