package engine.views;


import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.text.DecimalFormat;
import java.util.*;

import static engine.views.ConfigComponent.showMessage;

public class IndexingComponent {
    private SiteRepository siteRepository;
    private PageRepository pageRepository;
    private FieldRepository fieldRepository;
    private EntityManager entityManager;
    private VerticalLayout mainLayout;
    private Grid<Field> fieldGrid = new Grid<>(Field.class, false);
    private Grid<Page> grid = null;

    private ComboBox<String> cssSelectorComboBox = new ComboBox<>("CSS-селектор");
    private TextArea cssSelectorTextArea = new TextArea("Найдены элементы");
    private HorizontalLayout titleAndBodyHorizontalLayout = new HorizontalLayout();
    private VerticalLayout cssVerticalLayout = new VerticalLayout();

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
                case "HTML Блоки" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createHtmlBlocksContent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }
                    fieldGrid.setItems(fieldRepository.findAll());
                }
                case "Страницы сайта" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        content = createPageComponent();
                        contentsHashMap.put(label, content);
                        mainLayout.add(content);
                    }
                }
            }
            contentsHashMap.get(label).setVisible(true);
        });
        contentsHashMap.put("Страницы сайта", createPageComponent());
        mainLayout.add(contentsHashMap.get("Страницы сайта"));
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
                                showMessage(f.getName() + " нельзя удалять!",
                                        2000, Notification.Position.MIDDLE);
                            else
                                fieldRepository.delete(f);
                        });
                        fieldGrid.setItems(fieldRepository.findAll());
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
                showMessage("Введите Наименование", 1000, Notification.Position.MIDDLE);
            else {
                if (!(option == null)) {
                    option.setSelector(textFieldKey.getValue());
                    option.setName(textFieldName.getValue());
                    option.setWeight(Float.parseFloat(textFieldValue.getValue()));
                    try {
                        fieldRepository.save(option);
                    } catch (Exception exception) {
                        showMessage("Ошибка записи", 2000, Notification.Position.MIDDLE);
                        return;
                    }
                } else
                    try {
                        fieldRepository.save(new Field(
                                textFieldName.getValue(),
                                textFieldKey.getValue(),
                                Float.parseFloat(textFieldValue.getValue())));
                    } catch (Exception exception) {
                        showMessage("Ошибка записи!", 2000, Notification.Position.MIDDLE);
                        return;
                    }
                fieldGrid.setItems(fieldRepository.findAll());
                dialog.close();
            }
        });
        Button cancelButton = new Button("Отменить", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    @PostConstruct
    private VerticalLayout createHtmlBlocksContent() {
        var verticalLayout = new VerticalLayout();

        List<Button> buttons = createButtons(List.of("Добавить", "Редактировать", "Удалить"));
        verticalLayout.add(CreateUI.getTopLayout("Настройки индексации.", buttons), fieldGrid);

        fieldGrid.addComponentColumn(item -> {
            Checkbox checkbox = new Checkbox();
            checkbox.setValue(item.isActive());
            checkbox.addValueChangeListener(event -> {
                item.setActive(event.getValue());
                fieldRepository.save(item);
            });
            return checkbox;
        }).setHeader("Вкл.").setTextAlign(ColumnTextAlign.CENTER);

        fieldGrid.addColumn(Field::getName).setHeader("Наименование").setTextAlign(ColumnTextAlign.START).setSortable(true);
        fieldGrid.addColumn(Field::getSelector).setHeader("CSS-селектор").setTextAlign(ColumnTextAlign.CENTER).setSortable(true);
        fieldGrid.addColumn(Field::getWeight).setHeader("Коэффициент").setTextAlign(ColumnTextAlign.CENTER).setSortable(true);

        fieldGrid.setItems(fieldRepository.findAll());
        return verticalLayout;
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

            cssSelectorComboBox.clear();
            cssSelectorTextArea.clear();

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

                cssVerticalLayout.setEnabled(true);

                selectionEvent.getFirstSelectedItem().ifPresent(page -> {
                    String content = "";
                    content = page.getContent();
                    //Search titles
                    if (!content.isBlank()) {
                        //Set<String> titles = HtmlParsing.getHtmlElementsByRegEx("title\\W", content);
//                        if (!(titles == null)) {
//                            StringBuilder stringBuilder = new StringBuilder();
//                            titles.forEach(title -> stringBuilder.append(title).append('\n'));
//                            titleTextArea.setValue(stringBuilder.toString());
//                        }

                        List<Element> titles = HtmlParsing.getHtmlElementsByRegEx("title\\W", content);
                        if (titles.size() > 0) {
                            var stringBuilder = new StringBuilder();
                            for (Element element : titles) {
                                //stringBuilder.append(String.join(" ",
                                //        HtmlParsing.getRussianWords(title.toString())));

                                stringBuilder.append(HtmlParsing.getTagBody(element).concat("\n\n"));
                            }
                            titleTextArea.setValue(stringBuilder.toString().concat("\n"));

                        }

                        //Search Body
                        Document doc = Jsoup.parseBodyFragment(content);
                        bodyTextArea.setValue(doc.body().text());
                    }
                });
            });
        }

        jsoupStartButton.addClickListener(buttonClickEvent ->  {
            String content = grid.getSelectedItems().stream().findFirst().get().getContent();
            bodyTextArea.setValue(HtmlParsing.findElementsByCss(cssTextField.getValue(), content).toString());
        });

        //var titleAndBodyHorizontalLayout = new HorizontalLayout(titleTextArea, bodyTextArea);
        titleAndBodyHorizontalLayout.add(titleTextArea);
        titleAndBodyHorizontalLayout.setWidth("100%");
        fillCssLayout();
        return new  VerticalLayout(horizontalLayout, grid, cssVerticalLayout, titleAndBodyHorizontalLayout);
    }

    private void fillCssLayout() {
        cssVerticalLayout.setWidth("100%");

        cssSelectorComboBox.setItems(query -> {
            return fieldRepository.getAllNames(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });

        cssSelectorComboBox.addValueChangeListener(event->{
            String cssName = event.getValue();
            Field field = fieldRepository.findByName(cssName);

            String content = grid.getSelectedItems().stream().findFirst().get().getContent();
            Document document = Jsoup.parseBodyFragment(content);

            Elements elements = document.select(field.getSelector());

            cssSelectorTextArea.setValue(elements.text());

        });

        cssVerticalLayout = new VerticalLayout();

        cssSelectorTextArea.setWidth("100%");
        cssVerticalLayout.setSpacing(false);
        cssVerticalLayout.add(cssSelectorComboBox, cssSelectorTextArea);

        cssVerticalLayout.setEnabled(false);
    }

}
