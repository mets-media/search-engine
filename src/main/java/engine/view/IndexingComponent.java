package engine.view;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.dataview.GridLazyDataView;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.grid.dnd.GridDragEndEvent;
import com.vaadin.flow.component.grid.dnd.GridDragStartEvent;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import engine.dto.IdTextDto;
import engine.entity.*;
import engine.service.BeanAccess;
import engine.service.HtmlParsing;
import engine.service.Lemmatization;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static engine.service.Parser.insertOrUpdatePage;
import static engine.view.UIElement.showMessage;


public class IndexingComponent {

    private static BeanAccess beanAccess;
    private final VerticalLayout mainLayout;
    private final Grid<Field> fieldGrid = new Grid<>(Field.class, false);
    private Grid<Page> pageGrid = null;
    private Grid<KeepLink> errorGrid = null;
    private final ComboBox<String> cssSelectorComboBox = new ComboBox<>("CSS-селектор");
    private final TextArea cssSelectorTextArea = new TextArea("Результат");
    private VerticalLayout cssVerticalLayout = new VerticalLayout();
    private final HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();
    private List<KeepLink> draggedkeepLinkList;
    private KeepLink draggedKeepLinkItem;
    private Page draggedPageItemPage;
    private GridLazyDataView<Page> dataViewPage;
    //private GridLazyDataView<KeepLink> dataViewError;
    private GridListDataView<KeepLink> dataViewError;

    private ComboBox<Site> siteComboBox;
    private ComboBox<IdTextDto> errorComboBox;

    private final Grid<PartsOfSpeech> gridPartsOfSpeech = new Grid<>();

    public IndexingComponent() {
        mainLayout = UIElement.getMainLayout();
        mainLayout.add(UIElement.getTopLayout("Индексация", "xl", null));
        createTabs(List.of("HTML-поля", "Части речи", "Страницы сайта"));
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
            VerticalLayout content = null;
            switch (label) {
                case "HTML-поля" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createHtmlBlocksContent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }
                    fieldGrid.setItems(beanAccess.getFieldRepository().findAll());
                }
                case "Страницы сайта" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createPageComponent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }
                }
                case "Части речи" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createPartOfSpeechContent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }
                    gridPartsOfSpeech.setItems(beanAccess.getPartOfSpeechRepository().findAll());
                }
            }
            UIElement.hideAllVerticalLayouts(mainLayout);
            contentsHashMap.get(label).setVisible(true);
        });
        contentsHashMap.put("HTML-поля", createHtmlBlocksContent());
        mainLayout.add(contentsHashMap.get("HTML-поля"));
    }

    private List<Button> createButtons(List<String> captions) {
        List<Button> buttons = new ArrayList<>();
        for (String caption : captions) {

            var button = new Button(caption);
            button.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
            buttons.add(button);
            switch (button.getText()) {
                case "Добавить" -> {
                    button.addClickListener(buttonClickEvent -> {
                        showDialog(null);
                    });
                }
                case "Редактировать" -> {
                    button.addClickListener(buttonClickEvent -> {
                        Optional<Field> field = fieldGrid.getSelectedItems().stream().findFirst();
                        field.ifPresent(f -> {
                            showDialog(f);
                        });
                    });
                }
                case "Удалить" -> {
                    button.addClickListener(buttonClickEvent -> {
                        Optional<Field> field = fieldGrid.getSelectedItems().stream().findFirst();
                        field.ifPresent(f -> {
                            if (f.getId() < 0)
                                showMessage(f.getName() + " нельзя удалять!");
                            else
                                beanAccess.getFieldRepository().delete(f);
                        });
                        fieldGrid.setItems(beanAccess.getFieldRepository().findAll());
                    });
                }
            }
        }
        return buttons;
    }

    private void showDialog(Field option) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);

        dialog.setHeaderTitle("Новый CSS-селектор");

        HorizontalLayout horizontalLayout = new HorizontalLayout();

        TextField textFieldName = new TextField("Наименование");
        TextField textFieldKey = new TextField("CSS-селектор");
        TextField textFieldValue = new TextField("Коэффициент");

        horizontalLayout.add(textFieldName, textFieldKey, textFieldValue);
        dialog.add(horizontalLayout);

        if (!(option == null)) {
            dialog.setHeaderTitle("Редактировать CSS-селектор");
            textFieldKey.setValue(option.getSelector());
            textFieldName.setValue(option.getName());
            textFieldValue.setValue(option.getWeight().toString());
        }

        Button saveButton = new Button("Сохранить", e -> {
            if (textFieldName.isEmpty())
                showMessage("Введите Наименование");
            else {
                if (!(option == null)) {
                    option.setSelector(textFieldKey.getValue());
                    option.setName(textFieldName.getValue());
                    option.setWeight(Float.parseFloat(textFieldValue.getValue()));
                    try {
                        beanAccess.getFieldRepository().save(option);
                    } catch (Exception exception) {
                        showMessage("Ошибка записи");
                        return;
                    }
                } else
                    try {
                        beanAccess.getFieldRepository().save(new Field(
                                textFieldName.getValue(),
                                textFieldKey.getValue(),
                                Float.parseFloat(textFieldValue.getValue())));
                    } catch (Exception exception) {
                        showMessage("Ошибка записи!");
                        return;
                    }
                fieldGrid.setItems(beanAccess.getFieldRepository().findAll());
                dialog.close();
            }
        });
        Button cancelButton = new Button("Отменить", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private VerticalLayout createHtmlBlocksContent() {
        var verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        List<Button> buttons = createButtons(List.of("Добавить", "Редактировать", "Удалить"));
        verticalLayout.add(UIElement.getTopLayout("Поля на страницах сайтов", "m", buttons), fieldGrid);

        fieldGrid.addComponentColumn(item -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(item.isActive());
            checkbox.addValueChangeListener(event -> {
                item.setActive(event.getValue());
                beanAccess.getFieldRepository().save(item);
            });
            return checkbox;
        }).setHeader("Вкл.").setTextAlign(ColumnTextAlign.CENTER);

        fieldGrid.addColumn(Field::getName).setHeader("Наименование")
                .setTextAlign(ColumnTextAlign.START).setSortable(true);
        fieldGrid.addColumn(Field::getSelector).setHeader("CSS-селектор")
                .setTextAlign(ColumnTextAlign.CENTER).setSortable(true);
        fieldGrid.addColumn(Field::getWeight).setHeader("Коэффициент")
                .setTextAlign(ColumnTextAlign.CENTER).setSortable(true);

        fieldGrid.setItems(beanAccess.getFieldRepository().findAll());
        return verticalLayout;
    }

    public static void setDataAccess(BeanAccess beanAccess) {
        IndexingComponent.beanAccess = beanAccess;
    }

    private Grid<KeepLink> createErrorGrid() {
        Grid<KeepLink> grid = new Grid<>(KeepLink.class, false);

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        //UIElement.setAllCheckboxVisibility(grid,false);

        Grid.Column<KeepLink> col1 = grid.addColumn(KeepLink::getPath)
                .setHeader("Path")
                //.setKey("path")
                .setWidth("80%")
                .setResizable(true);
        //.setAutoWidth(true);

        Grid.Column<KeepLink> col2 = grid.addColumn(KeepLink::getCode)
                .setHeader("Code")
                //.setKey("code")
                .setWidth("10%")
                .setResizable(true)
                .setAutoWidth(true);
        col2.setVisible(false);

        HeaderRow headerRow = grid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText("Страницы с ошибками");
        simpleCell.getElement().getStyle().set("text-align", "start");
        headerRow.join(col1, col2).setComponent(simpleCell);

        grid.addItemDoubleClickListener(pageItemDoubleClickEvent -> StartBrowser.
                startBrowser(pageItemDoubleClickEvent.getItem().getPath()));

        return grid;
    }

    private ComboBox<IdTextDto> createErrorComboBox() {
        ComboBox<IdTextDto> comboBox = new ComboBox<>("Тип ошибки");
        comboBox.setItemLabelGenerator(IdTextDto::text);

        comboBox.addValueChangeListener(event -> {
            int siteId = siteComboBox.getValue().getId();
            int code = event.getValue().id();

            List<KeepLink> list;
            if (code == 0)  //все ошибки
                list = beanAccess.getKeepLinkRepository()
                        .findBySiteIdAndStatus(siteId, LinkStatus.ERROR_LINK.ordinal());
            else
                list = beanAccess.getKeepLinkRepository()
                        .findBySiteIdAndCodeAndStatus(siteId, code, LinkStatus.ERROR_LINK.ordinal());

            dataViewError = errorGrid.setItems(list);

            errorGrid.getColumns().get(0).setHeader(errorComboBox.getValue().text());
            errorGrid.getHeaderRows().get(0).getCell(errorGrid.getColumns().get(0))
                    .setText("Страницы с ошибками " + " [ " + list.size() + " стр.]");
        });
        return comboBox;
    }

    private void handleDragStartPage(GridDragStartEvent<Page> e) {
        draggedPageItemPage = e.getDraggedItems().get(0);
    }

    private void handleDragEndPage(GridDragEndEvent<Page> e) {
        draggedPageItemPage = null;
    }

    private void handleDragStartKeepLink(GridDragStartEvent<KeepLink> e) {
        //draggedKeepLinkItem = e.getDraggedItems().get(0);

        if (errorGrid.getSelectedItems().size() > 1)
            draggedkeepLinkList = errorGrid.getSelectedItems().stream().toList();
        else
            draggedKeepLinkItem = e.getDraggedItems().get(0);

    }

    private void handleDragEndKeepLink(GridDragEndEvent<KeepLink> e) {
        draggedKeepLinkItem = null;
        draggedkeepLinkList = null;
    }

    private void dragAndDropGrids() {

        errorGrid.setDropMode(GridDropMode.ON_GRID);
        errorGrid.setRowsDraggable(true);
        errorGrid.addDragStartListener(this::handleDragStartKeepLink);
        errorGrid.addDropListener(e -> {
            //dataViewPage.removeItem(draggedItem);
            //dataViewError.addItem(draggedItem);
        });
        errorGrid.addDragEndListener(this::handleDragEndKeepLink);

        pageGrid.setDropMode(GridDropMode.ON_GRID);
        pageGrid.setRowsDraggable(false);
        pageGrid.addDragStartListener(this::handleDragStartPage);
        pageGrid.addDropListener(e -> {
            //dataView1.removeItem(draggedItem);
            //dataView2.addItem(draggedItem);
            if (draggedkeepLinkList != null) {/** Запись выбранных KeepLink */
                for (KeepLink keepLink : draggedkeepLinkList) {
                    if (!(insertOrUpdatePage(keepLink.getPath(), beanAccess))) {
                        showMessage("Страница за пределами проиндексированных сайтов! " + keepLink.getPath());
                    }
                    showMessage(("Записываем и индексируем страницу: ").concat(keepLink.getPath()));
                    dataViewError.removeItem(keepLink);
                }
            } else {
                if (!(insertOrUpdatePage(draggedKeepLinkItem.getPath(), beanAccess))) {
                    showMessage("Страница за пределами проиндексированных сайтов! " + draggedKeepLinkItem.getPath());
                }
                showMessage(("Записываем и индексируем страницу: ").concat(draggedKeepLinkItem.getPath()));
                dataViewError.removeItem(draggedKeepLinkItem);
            }
        });
        pageGrid.addDragEndListener(this::handleDragEndPage);


    }

    private ComboBox<Site> createSiteComboBox() {

        ComboBox<Site> siteComboBox = new ComboBox<>("Сайт:");
        siteComboBox.setItemLabelGenerator(Site::getUrl);

        siteComboBox.setItems(query -> {
            return beanAccess.getSiteRepository().getSitesFromPageTable(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });
        //==================================================================================
        siteComboBox.addValueChangeListener(event -> {
//            cssSelectorComboBox.clear();
//            cssSelectorTextArea.clear();
//            cssSelectorTextArea.setReadOnly(true);

            Site site = event.getValue();

            Integer pageCount = beanAccess.getPageRepository().countBySiteId(site.getId());
            site.setPageCount(pageCount);

            //pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));

            pageGrid.getHeaderRows().get(0).getCell(pageGrid.getColumns().get(0))
                    .setText("Индексированные страницы [ " + new DecimalFormat("#,###")
                            .format(pageCount) + " стр.]");

            dataViewPage = pageGrid.setItems(query -> beanAccess.getPageRepository()
                    .findBySiteId(
                            site.getId(),
                            PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("path")))
                    .stream());

//            dataViewError = errorGrid.setItems(query ->
//                    beanAccess.getKeepLinkRepository().findBySiteId(site.getId(),
//                                    PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("code")))
//                            .stream());

            //dataViewError = errorGrid.setItems(beanAccess.getKeepLinkRepository().findBySiteId(site.getId()));

            List<IdTextDto> list = beanAccess.getKeepLinkRepository()
                    .getDistinctErrors(site.getId(), LinkStatus.ERROR_LINK.ordinal());

//            List<KeepLink> list = beanAccess.getImplRepository()
//                    .getErrorNames(site.getId(),LinkStatus.ERROR_LINK.ordinal());

//            List<IdTextDto> list = beanAccess.getKeepLinkRepository()
//                    .getDistinctErrorCode(site.getId(),LinkStatus.ERROR_LINK.ordinal());

            list.add(0, new IdTextDto(0, "Все ошибки"));
            errorComboBox.setItems(list);
            errorComboBox.setValue(list.get(0));

        });//===============================================================================

        siteComboBox.setWidth("50%");
        return siteComboBox;
    }


    private Grid<Page> createPageGrid() {

        //pageCountTextField.setReadOnly(true);

        Grid<Page> grid = new Grid<>(Page.class, false);

        Grid.Column<Page> col1 = grid.addColumn(Page::getCode)
                .setHeader("Code")
                .setTextAlign(ColumnTextAlign.CENTER)
                .setWidth("10%")
                .setResizable(true);
        col1.setVisible(false);

        Grid.Column<Page> col2 = grid.addColumn(Page::getPath)
                .setHeader("code [200]")
                .setKey("path")
                .setWidth("90%")
                .setResizable(true)
                .setAutoWidth(true);

        HeaderRow headerRow = grid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText("Индексированные страницы");
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(col1, col2).setComponent(simpleCell);


        grid.addSelectionListener(selectionEvent -> {
            cssSelectorComboBox.clear();
            cssSelectorTextArea.clear();
        });

        grid.addItemDoubleClickListener(pageItemDoubleClickEvent -> {
            StartBrowser.startBrowser(pageItemDoubleClickEvent.getItem().getPath());
        });
        return grid;
    }

    private VerticalLayout createPageComponent() {

        pageGrid = createPageGrid();
        errorGrid = createErrorGrid();
        dragAndDropGrids();
        //fillCssLayout();

        siteComboBox = createSiteComboBox();

        errorComboBox = createErrorComboBox();


        siteComboBox.setWidth("50%");
        errorComboBox.setWidth("50%");

        var horizontalLayout = new HorizontalLayout(siteComboBox, errorComboBox);
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        horizontalLayout.setSizeFull();

        return new VerticalLayout(horizontalLayout,
                UIElement.getVerticalLayoutWithSideBySideGrids(List.of(pageGrid, errorGrid), "gridsLayout"));
        //cssVerticalLayout);

    }

    private void fillCssLayout() {
        cssVerticalLayout.setWidth("100%");

        cssSelectorComboBox.setItems(query -> {
            return beanAccess.getFieldRepository().getAllNames(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });

        cssSelectorComboBox.addValueChangeListener(event -> {
            String cssName = event.getValue();

            if ((cssName == null) || (cssName.isBlank()))
                return;

            String content = pageGrid.getSelectedItems().stream().findFirst().get().getContent();
            Document document = Jsoup.parseBodyFragment(content);

            Field field = beanAccess.getFieldRepository().findByName(cssName);
            switch (field.getSelector()) {
                case "title" -> {
                    cssSelectorTextArea.setValue(document.title());
                }
                case "body" -> {
                    cssSelectorTextArea.setValue(document.body().text());
                }
                default -> {
                    Elements elements = document.select(field.getSelector());

                    StringBuilder stringBuilder = new StringBuilder();
                    elements.forEach(element -> {
                        //stringBuilder.append(element.toString().concat("\n"));
                        stringBuilder.append(HtmlParsing.getTagBody(element).concat("\n"));
                    });
                    cssSelectorTextArea.setValue(stringBuilder.toString().replace("&nbsp;", " "));
                }
            }
        });

        Button lemmaButton = new Button("Лемматизатор");
        lemmaButton.addClickListener(buttonClickEvent -> {

            List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());

            Lemmatization lemma = new Lemmatization(excludeList, null);

            HashMap<String, Integer> lemmaHashMap = lemma.getLemmaHashMap(cssSelectorTextArea.getValue());

            StringBuilder stringBuilder = new StringBuilder();
            lemmaHashMap.entrySet().forEach(x -> {
                stringBuilder.append(x.getKey() + " -> " + x.getValue() + "\n");
            });
            cssSelectorTextArea.setValue(stringBuilder.toString());
        });

        Button indexingButton = new Button("Индексация");
        indexingButton.addClickListener(buttonClickEvent -> {
            cssSelectorComboBox.setValue("");
            List<String> excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());

            Lemmatization lemmatization = new Lemmatization(excludeList,
                    beanAccess.getFieldRepository().findByActive(true));

            pageGrid.getSelectedItems().stream().findFirst().ifPresent(page -> {
                var list =
                        lemmatization.getHashMapsLemmaForEachCssSelector(page.getContent());

                StringBuilder stringBuilder = new StringBuilder();

                //Количество лемм для каждого cssSelector
                for (int i = 0; i < list.size(); i++) {
                    stringBuilder.append(lemmatization.getCssSelectors().get(i).getSelector() + ": " + list.get(i).size() + " лемм\n");
                }
                stringBuilder.append("---\n");

                HashMap<String, Lemmatization.LemmaInfo> hm = lemmatization.mergeAllHashMaps(list);
                hm.entrySet().forEach(e -> stringBuilder.append(e.getValue().getLemma() + "," +
                        e.getValue().getCount() + ", " + e.getValue().getRank() + "\n\n"));

                //stringBuilder.append("\n\n Size после BiFunction: " + hm.size() + "\n\n");

                cssSelectorTextArea.setValue(stringBuilder.toString());
            });
        });


        var cssControlsLayout = new HorizontalLayout(cssSelectorComboBox, lemmaButton, indexingButton);
        cssControlsLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        //pageGrid = createPageGrid();

        cssVerticalLayout = new VerticalLayout();

        cssSelectorTextArea.setWidth("100%");
        cssVerticalLayout.setSpacing(false);
        //cssVerticalLayout.add(cssControlsLayout, cssSelectorTextArea, pageGrid);
        cssVerticalLayout.add(cssControlsLayout, cssSelectorTextArea);

        cssVerticalLayout.setEnabled(true);
    }

    private VerticalLayout createPartOfSpeechContent() {
        var vLayout = new VerticalLayout();
        vLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);

        gridPartsOfSpeech.addThemeVariants(GridVariant.LUMO_COMPACT);
        gridPartsOfSpeech.setSelectionMode(Grid.SelectionMode.SINGLE);

        gridPartsOfSpeech.addComponentColumn(item -> {
                    Checkbox checkbox = new Checkbox();
                    checkbox.setValue(item.getInclude());
                    checkbox.addValueChangeListener(event -> {
                        item.setInclude(event.getValue());
                        beanAccess.getPartOfSpeechRepository().save(item);
                    });
                    return checkbox;
                }).setHeader("Вкл").setAutoWidth(true)
                .setSortable(false

                )
                .setWidth("10%")
                .setTextAlign(ColumnTextAlign.CENTER);
        gridPartsOfSpeech.addColumn(PartsOfSpeech::getName)
                .setHeader("Наименование").setAutoWidth(true).setSortable(true);
        gridPartsOfSpeech.addColumn(PartsOfSpeech::getShortName)
                .setHeader("Признак").setAutoWidth(true).setSortable(true);

        vLayout.add(gridPartsOfSpeech);
        return vLayout;
    }

}
