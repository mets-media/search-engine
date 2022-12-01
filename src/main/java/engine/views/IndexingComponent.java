package engine.views;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import engine.entity.Field;
import engine.entity.Page;
import engine.repository.FieldRepository;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import engine.service.HtmlParsing;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class IndexingComponent {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private FieldRepository fieldRepository;
    private EntityManager entityManager;
    private VerticalLayout mainLayout;
    private Grid<Field> fieldGrid = new Grid<>(Field.class, false);
    private Grid<Page> grid = null;

    private TextField pageCountTextField = new TextField("Страниц в базе данных");

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

                Integer pageCount = pageRepository.countBySiteId(site.getId());
                site.setPageCount(pageCount);
                pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));

                grid.setItems(query -> pageRepository
                        .findBySiteId(
                                site.getId(),
                                PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("path")))
                        .stream());
            });

        });//===========================================================================================================


        var horizontalLayout = new HorizontalLayout();
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);

        TextField cssTextField = new TextField("css query:");
        Button jsoupStartButton = new Button("Найти");

        horizontalLayout.add(siteComboBox, pageCountTextField, cssTextField, jsoupStartButton);

        TextArea titleTextArea = new TextArea("Title");
        titleTextArea.setWidth("100%");
        titleTextArea.setHeight("50%");

        TextArea bodyTextArea = new TextArea("Body");
        bodyTextArea.setWidth("100%");
        bodyTextArea.setHeight("50%");

        if (grid == null) {
            pageCountTextField.setReadOnly(true);

            grid = new Grid<>(Page.class, false);

            grid.addColumn(Page::getPath)
                    .setHeader("Path")
                    .setKey("path")
                    .setResizable(true);
            grid.addColumn(Page::getCode)
                    .setHeader("Code")
                    .setAutoWidth(true);

            grid.addSelectionListener(selectionEvent -> {
                selectionEvent.getFirstSelectedItem().ifPresent(page -> {
                    String content = "";
                    content = page.getContent();
                    //Search titles
                    if (!content.isBlank()) {
                        Set<String> titles = HtmlParsing.getAllTitles(content);

                        if (!(titles == null)) {
                            StringBuilder stringBuilder = new StringBuilder();
                            titles.forEach(title -> stringBuilder.append(title).append('\n'));
                            titleTextArea.setValue(stringBuilder.toString());
                        }
                    }
                    //Search Body
                    Document doc = Jsoup.parseBodyFragment(content);
                    bodyTextArea.setValue(doc.body().text());
                });
            });
        }

        jsoupStartButton.addClickListener(buttonClickEvent -> {
            String content = grid.getSelectedItems().stream().findFirst().get().getContent();
            Set<String> titles = HtmlParsing.getAllTitles(content);

            if (!(titles == null)) {
                StringBuilder stringBuilder = new StringBuilder();
                titles.forEach(title -> stringBuilder.append(title).append('\n'));
                titleTextArea.setValue(stringBuilder.toString());
            }
        });

        HorizontalLayout titleAndBodyHorizontalLayout = new HorizontalLayout(titleTextArea, bodyTextArea);
        titleAndBodyHorizontalLayout.setWidth("100%");
        return new VerticalLayout(horizontalLayout, grid, titleAndBodyHorizontalLayout);

    }

}
