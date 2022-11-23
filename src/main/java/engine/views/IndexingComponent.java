package engine.views;


import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import engine.entity.Field;
import engine.entity.Page;
import engine.entity.Site;
import engine.repository.FieldRepository;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IndexingComponent {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private FieldRepository fieldRepository;
    private VerticalLayout mainLayout;
    private Grid<Field> fieldGrid = new Grid<>(Field.class, false);
    private Grid<Page> pageGrid = new Grid<>(Page.class, false);

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

    public void dataAccess(FieldRepository fieldRepository, PageRepository pageRepository, SiteRepository siteRepository) {
        this.fieldRepository = fieldRepository;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
    }

    private VerticalLayout createPageComponent() {
//        ComboBox<String> siteComboBox = new ComboBox<>();
//        List<String> siteList = new ArrayList<>();
//        for (Site site : siteRepository.findAll()) {
//            String url = site.getUrl();
//            siteList.add(url);
//        }
//        siteComboBox.setItems(siteList);
//        return new VerticalLayout(siteComboBox);

        pageGrid.addColumn(Page::getPath).setHeader("Path");
        pageGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        pageGrid.setPageSize(10);

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

        VerticalLayout leftVLayout = new VerticalLayout(pageGrid);
        leftVLayout.setSizeFull();

        TextField textField = new TextField("Результат");
        textField.setSizeFull();
        VerticalLayout rightVLayout = new VerticalLayout(textField);


        return new VerticalLayout();
    }
}
