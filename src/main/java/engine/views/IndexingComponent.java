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
import engine.repository.PartOfSpeechRepository;
import engine.repository.SiteRepository;
import engine.service.HtmlParsing;
import engine.service.Lemmatization;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityManager;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static engine.views.ConfigComponent.showMessage;


public class IndexingComponent {
    private static PartOfSpeechRepository partOfSpeechRepository;
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static FieldRepository fieldRepository;
    private static EntityManager entityManager;
    private final VerticalLayout mainLayout;
    private final Grid<Field> fieldGrid = new Grid<>(Field.class, false);
    private Grid<Page> grid = null;
    private final ComboBox<String> cssSelectorComboBox = new ComboBox<>("CSS-селектор");
    private final TextArea cssSelectorTextArea = new TextArea("Результат");
    private VerticalLayout cssVerticalLayout = new VerticalLayout();
    private final TextField pageCountTextField = new TextField("Страниц в базе данных");
    private final HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();

    public IndexingComponent() {
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Настройки индексации", "xl", null));
        createTabs(List.of("HTML-поля", "Страницы сайта"));
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
            CreateUI.hideAllVerticalLayouts(mainLayout);
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

    private VerticalLayout createHtmlBlocksContent() {
        var verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        List<Button> buttons = createButtons(List.of("Добавить", "Редактировать", "Удалить"));
        verticalLayout.add(CreateUI.getTopLayout("Поля на страницах сайтов", "m", buttons), fieldGrid);

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

    public static void dataAccess(FieldRepository fRepository,
                           PageRepository pRepository,
                           SiteRepository sRepository,
                           PartOfSpeechRepository posRepository,
                           EntityManager entityManager) {
        fieldRepository = fRepository;
        pageRepository = pRepository;
        siteRepository = sRepository;
        partOfSpeechRepository = posRepository;
        entityManager = entityManager;
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
            cssSelectorTextArea.setReadOnly(true);

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

        horizontalLayout.add(siteComboBox, pageCountTextField);

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
                cssSelectorComboBox.clear();
                cssSelectorTextArea.clear();
            });
        }

        fillCssLayout();
        return new VerticalLayout(horizontalLayout, grid, cssVerticalLayout);
    }
    private void fillCssLayout() {
        cssVerticalLayout.setWidth("100%");

        cssSelectorComboBox.setItems(query -> {
            return fieldRepository.getAllNames(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });

        cssSelectorComboBox.addValueChangeListener(event -> {
            String cssName = event.getValue();

            if ((cssName == null) || (cssName.isBlank()))
                return;

            String content = grid.getSelectedItems().stream().findFirst().get().getContent();
            Document document = Jsoup.parseBodyFragment(content);

            Field field = fieldRepository.findByName(cssName);
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

            List<String> excludeList = partOfSpeechRepository.findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());

            Lemmatization lemma = new Lemmatization(excludeList, null);

            HashMap<String, Integer> lemmaHashMap = lemma.getLemmaCount(cssSelectorTextArea.getValue());

            StringBuilder stringBuilder = new StringBuilder();
            lemmaHashMap.entrySet().forEach(x -> {
                stringBuilder.append(x.getKey() + " -> " + x.getValue() + "\n");
            });
            cssSelectorTextArea.setValue(stringBuilder.toString());
        });

        Button indexingButton = new Button("Индексация");
        indexingButton.addClickListener(buttonClickEvent ->  {
            cssSelectorComboBox.setValue("");
            List<String> excludeList = partOfSpeechRepository.findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());

            Lemmatization lemmatization = new Lemmatization(excludeList, fieldRepository.findByActive(true));

            grid.getSelectedItems().stream().findFirst().ifPresent(page -> {
                List<HashMap<String, Lemmatization.LemmaInfo>> list =
                        lemmatization.getHashMapsLemmaForEachCssSelector(page.getContent());

                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < list.size();i++) {
                    stringBuilder.append(list.get(i).size() + "\n\n");
                }

                HashMap<String, Lemmatization.LemmaInfo> hm = lemmatization.mergeAllHashMaps(list);
                hm.entrySet().forEach(e -> stringBuilder.append(e.getValue().getLemma() + "," +
                        e.getValue().getCount() + ", " + e.getValue().getRank()+"\n\n"));

                //stringBuilder.append("\n\n Size посе BiFunction: " + hm.size() + "\n\n");

                cssSelectorTextArea.setValue(stringBuilder.toString());
            });
        });


        var cssControlsLayout = new HorizontalLayout(cssSelectorComboBox, lemmaButton, indexingButton);
        cssControlsLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        cssVerticalLayout = new VerticalLayout();

        cssSelectorTextArea.setWidth("100%");
        cssVerticalLayout.setSpacing(false);
        cssVerticalLayout.add(cssControlsLayout, cssSelectorTextArea);

        cssVerticalLayout.setEnabled(false);
    }


}
