package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.function.ValueProvider;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.repository.ConfigRepository;
import engine.repository.FieldRepository;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import engine.service.HtmlParsing;
import engine.service.Parser;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Getter
public class SiteComponent {

    private final VerticalLayout verticalLayout;
    private final Grid<Site> grid;
    private static ConfigRepository configRepository;
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static FieldRepository fieldRepository;
    private static JdbcTemplate jdbcTemplate;


    public SiteComponent() {
        verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);
        verticalLayout.setMinHeight("100%");

        grid = new Grid<>(Site.class, false);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        setAllCheckboxVisibility(grid, true);

        //grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(Site::getName).setHeader("Наименование").setResizable(true);
        grid.addColumn(Site::getUrl).setHeader("Адрес(url)").setResizable(true);
        grid.addColumn(Site::getPageCount).setHeader("Страниц в базе").setResizable(true);
        grid.addColumn(Site::getStatus).setHeader("Статус").setResizable(true);
        //grid.addColumn(Site::getStatusTime).setHeader("Время").setResizable(true);


//        grid.addColumn(new LocalDateTimeRenderer<>(new ValueProvider<Site, LocalDateTime>() {
//            @Override
//            public LocalDateTime apply(Site site) {
//                return site.getStatusTime();
//            }
//        }));


//        grid.addColumn(new LocalDateTimeRenderer<Site>(new ValueProvider<Site, LocalDateTime>() {
//            @Override
//            public LocalDateTime apply(Site site) {
//                return site.getStatusTime();
//            }
//        })).setHeader("преобразование времени").setResizable(true);


        grid.addColumn(new LocalDateTimeRenderer<>((ValueProvider<Site, LocalDateTime>) site ->
                site.getStatusTime())).setHeader("Дата статуса ").setResizable(true);

//        grid.addColumn(new LocalDateTimeRenderer<>((ValueProvider<Site, LocalDateTime>) Site::getStatusTime))
//                .setHeader("Дата статуса").setResizable(true);


        grid.addColumn(Site::getLastError).setHeader("Сообщение").setResizable(true);

        //Создание кнопок управления
        HorizontalLayout hLayout = createButtons();

        verticalLayout.add(hLayout);
        verticalLayout.add(grid);

    }

    private HorizontalLayout createButtons() {
        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout horizontalLayoutForLabel = new HorizontalLayout();
        horizontalLayoutForLabel.setAlignItems(FlexComponent.Alignment.START);
        horizontalLayoutForLabel.setSizeUndefined();

        Label label = new Label("Анализ информации на страницах сайтов");
        label.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin", "0");

        horizontalLayoutForLabel.add(label);

        //========================= ТЕСТ ==========================================
        Button testButton = new Button("Тест");
        testButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        testButton.addClickListener(event -> {
            fieldRepository.initData();
            //generateDialog("Генерация dialog из Grid", grid, 3);
            //updateSiteInfo();
        });
        //========================= ДОБАВИТЬ САЙТ ==========================================
        Button createButton = new Button("Добавить");
        createButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        createButton.addClickListener(buttonClickEvent -> {
            showNewSiteDialog();
        });

        //============================  Кнопка удаления Сайта  =================================
        Button deleteButton = new Button("Удалить");
        deleteButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        deleteButton.addClickListener(buttonClickEvent -> {
            List<Site> sitesForDelete = grid.getSelectedItems().stream().collect(Collectors.toList());
            if (sitesForDelete.isEmpty()) {
                ConfigComponent.showMessage("Не выбраны сайты для удаления", 1000, Notification.Position.MIDDLE);
                return;
            }
            showDeleteSiteDialog(sitesForDelete);
        });


        //========================= СКАНИРОВАТЬ САЙТ ==========================================
        Button parseButton = new Button("Сканировать");
        parseButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        parseButton.addClickListener(buttonClickEvent -> {
            Parser.setDataAccess(configRepository, siteRepository, pageRepository, jdbcTemplate);

            Set<Site> selectedSites = grid.getSelectedItems();
            selectedSites.forEach(site -> {
                grid.deselect(site); //после модификации - другой "site" - выделение не снимется
                Parser.getStopList().remove(site);

                site.setStatus(SiteStatus.DOWNLOADING);
                siteRepository.save(site);
                Parser.start(site);
            });
            grid.setItems(siteRepository.findAll());

            //setContent(getSimpleGrid());
        });

        //========================= СТОП СКАНИРОВАНИЕ ==========================================
        Button stopButton = new Button("Стоп!");
        stopButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        stopButton.addClickListener(event -> {
            Set<Site> stopSites = grid.getSelectedItems();
            stopSites.forEach(site -> {
                //parser.stopScanSite(site);
                Parser.stop(site); //Новый вариант
                grid.deselect(site);
                site.setStatus(SiteStatus.STOPPED);
                siteRepository.save(site);
            });
            //grid.getDataProvider().refreshAll();
            grid.setItems(siteRepository.findAll());
        });

        hLayout.add(horizontalLayoutForLabel, testButton, createButton, deleteButton, parseButton, stopButton);
        return hLayout;
    }

    public static void setAllCheckboxVisibility(Grid<Site> grid, boolean visible) {
        if (visible) {
            ((GridMultiSelectionModel<?>) grid.getSelectionModel())
                    .setSelectAllCheckboxVisibility(
                            GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE
                    );
        } else
            ((GridMultiSelectionModel<?>) grid.getSelectionModel())
                    .setSelectAllCheckboxVisibility(
                            GridMultiSelectionModel.SelectAllCheckboxVisibility.HIDDEN
                    );
    }

    private void showDeleteSiteDialog(List<Site> sites) {
        Dialog dialog = new Dialog();
        //dialog.setMaxHeight(300, Unit.PIXELS);
        dialog.setMaxHeight("30%");

        Button confirm = new Button("Удалить");
        confirm.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        Button cancel = new Button("Отмена");
        cancel.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        dialog.setHeaderTitle("Удалить выбранные сайты?");
        dialog.getFooter().add(cancel, confirm);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);

        for (Site site : sites) {
            verticalLayout.add(new Label(site.getUrl()));
        }

        dialog.add(verticalLayout);

        confirm.addClickListener(clickEvent -> {

            sites.forEach(delSite -> {
                new Thread() {
                    public void run() {
                        pageRepository.deleteBySiteId(delSite.getId());
                    }
                }.start();

                siteRepository.delete(delSite);

                try {
                    FileUtils.deleteDirectory(new File("data/" + HtmlParsing.getDomainName(delSite.getUrl())));
                } catch (IOException e) {
                    //throw new RuntimeException(e);
                }

            });

            dialog.close();
            Notification notification = new Notification("Удалени выполнено!", 1000);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.open();
            grid.setItems(siteRepository.findAll());
        });
        cancel.addClickListener(clickEvent -> {
            dialog.close();
        });
        dialog.open();
    }

    private void generateDialog(String title, Grid<Site> sourceGrid, int countFirstVisibleFields) {
        Dialog dialog = new Dialog();
        dialog.setMinWidth("30%");

        dialog.setHeaderTitle(title);

        Button confirmButton = new Button("Сохранить");
        Button cancelButton = new Button("Отменить", e -> dialog.close());
        dialog.getFooter().add(confirmButton, cancelButton);

        VerticalLayout verticalLayout = new VerticalLayout();

        List<Grid.Column<Site>> columns = sourceGrid.getColumns();

        List<TextField> fields = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        int fieldsCount = 0;
        for (Grid.Column<Site> column : columns) {
            if (fieldsCount >= countFirstVisibleFields) {
                break;
            }
            String titleString = column.getElement().getChild(0).toString();
            titleString = titleString.substring(titleString.indexOf(">") + 1, titleString.lastIndexOf("<"));

            titles.add(titleString);
            TextField textField = new TextField(titleString);
            fields.add(textField);

            verticalLayout.add(textField);

            fieldsCount++;
        }


        dialog.add(verticalLayout);

        dialog.open();
    }

    private void showNewSiteDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setMinWidth("30%");

        dialog.setHeaderTitle("Добавить сайт");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        TextField textFieldName = new TextField("Наименование");
        textFieldName.setMinWidth("50%");
        TextField textFieldUrl = new TextField("url сайта [http://....]");
        //textFieldUrl.setValue("http://");
        textFieldUrl.setWidth("50%");
        horizontalLayout.add(textFieldName,textFieldUrl);
        dialog.add(horizontalLayout);

        Button saveButton = new Button("Сохранить", e -> {
            if (textFieldUrl.isEmpty())
                ConfigComponent.showMessage("URL сайта не может быть пустым!", 1000, Notification.Position.MIDDLE);
            else {
                String newUrl = textFieldUrl.getValue();

                if (newUrl.charAt(newUrl.length() - 1) == '/') newUrl = newUrl.substring(0, newUrl.length() - 1);

                Site site = new Site();
                site.setName(textFieldName.getValue());
                site.setUrl(newUrl);
                site.setStatus(SiteStatus.NEW_SITE);
                site.setStatusTime(LocalDateTime.now());
                site.setPageCount(0);
                siteRepository.save(site);

                grid.setItems(siteRepository.findAll());
                dialog.close();
            }
        });
        Button cancelButton = new Button("Отменить", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void updateSiteInfo() {

        grid.getSelectedItems().forEach(site -> {
            int pageCount = pageRepository.countBySiteId(site.getId());
            site.setPageCount(pageCount);
            siteRepository.save(site);
            grid.setItems(siteRepository.findAll());
        });
    }
    public static void setDataAccess(ConfigRepository configRepository, SiteRepository siteRepository, PageRepository pageRepository, FieldRepository fieldRepository, JdbcTemplate jdbcTemplate) {
        SiteComponent.configRepository = configRepository;
        SiteComponent.siteRepository = siteRepository;
        SiteComponent.pageRepository = pageRepository;
        SiteComponent.fieldRepository = fieldRepository;
        SiteComponent.jdbcTemplate = jdbcTemplate;
    }
}
