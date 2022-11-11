package engine.views;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;

import com.vaadin.flow.router.Route;

import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.service.Parser;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import engine.service.RefreshGridTimer;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.stream.Collectors;

@Route
@Getter
public class MainView extends AppLayout {
    private static final Grid<Site> grid = new Grid<>(Site.class, false);
    String selectedSite = "";
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    Parser parser;
    VerticalLayout siteComponent;

    public MainView() {

        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("Search Engine");
        title.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

//        Tab tab = new Tab("Список сайтов");
//        tab.getElement().addEventListener("click", domEvent -> setContent(getSitesComponent()));
//        tabs.add(tab);

        Tab tab = new Tab("Сайты");
        tab.getElement().addEventListener("click", domEvent -> {
            if (getContent() == null)
                setContent(getSimpleGrid());
        });
        tabs.add(tab);

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }

    public static Grid getGrid() {
        return grid;
    }

    public static void gridRefresh() {
        grid.getDataProvider().refreshAll();
    }

    public static void gridRefreshSite(int siteId) {
        //Site site = siteRepository.getById(siteId);
        //grid.getDataProvider().refreshItem(site);
    }

    private HorizontalLayout createButton() {
        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Button testButton = new Button("Тест");
        testButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        testButton.addClickListener(event -> {
            grid.getDataProvider().refreshAll();

        });

        Button createButton = new Button("Добавить сайт");
        createButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        createButton.addClickListener(buttonClickEvent -> {
            setContent(getModifyComponent(0));
        });

        //=================  Кнопка удаления Сайта  =======================
        Button deleteButton = new Button("Удалить");
        deleteButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        deleteButton.addClickListener(buttonClickEvent -> {
            List<Site> sitesForDelete = grid.getSelectedItems().stream().collect(Collectors.toList());
            if (sitesForDelete.isEmpty()) {
                showMessage("Не выбраны сайты для удаления", 1000, Notification.Position.MIDDLE);
                return;
            }
            Dialog dialog = new Dialog();
            Button confirm = new Button("Удалить");
            Button cancel = new Button("Отмена");

            dialog.add("Удалить выбранные сайты?");
            dialog.add(confirm);
            dialog.add(cancel);
            confirm.addClickListener(clickEvent -> {

                //Optional<Site> site = grid.getSelectedItems().stream().findFirst();
                //siteRepository.delete(site.get());

                sitesForDelete.forEach(delSite -> siteRepository.delete(delSite));

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
        });

        Button parseButton = new Button("Сканировать");
        parseButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        parseButton.addClickListener(buttonClickEvent -> {
            Parser.setPageRepository(pageRepository);
            Parser.setSiteRepository(siteRepository);
            Parser.setJdbcTemplate(jdbcTemplate);

            Set<Site> selectedSites = grid.getSelectedItems();
            selectedSites.forEach(site -> {
                grid.deselect(site); //после модификации - другой "site" - выделение не снимется
                Parser.getStopList().remove(site);

                site.setStatus(SiteStatus.DOWNLOADING);
                siteRepository.save(site);
                Parser.start(site);
            });
            grid.setItems(siteRepository.findAll());

            //timerStart(1000L);
            setContent(getSimpleGrid());
        });

        Button stopButton = new Button("Стоп!");
        stopButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        stopButton.addClickListener(event -> {
            Set<Site> stopSites = grid.getSelectedItems();
                stopSites.forEach(site -> {
                    parser.stopScanSite(site);
                    grid.deselect(site);
                    site.setStatus(SiteStatus.STOPPED);
                    siteRepository.save(site);
                });
            //grid.getDataProvider().refreshAll();
            grid.setItems(siteRepository.findAll());
        });

        hLayout.add(testButton, createButton, deleteButton, parseButton, stopButton);
        return hLayout;
    }

    private static Timer timerStart(Long delay) {
        Timer timer = new Timer("Timer");
        timer.schedule(new RefreshGridTimer(grid), delay);
        return timer;
    }

    private void setAllCheckboxVisibility(Grid grid, boolean visible) {
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

    private VerticalLayout getSimpleGrid() {
        if (!(siteComponent == null))
            return siteComponent;

        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        setAllCheckboxVisibility(grid, true);
        grid.addItemClickListener(event -> {
            //selectedSite = event.getItem().getUrl();
            showMessage("Сайт: " + event.getItem().getUrl(), 1000, Notification.Position.MIDDLE);
            //grid.getEditor().editItem(event.getItem());
        });

        grid.addColumn(Site::getUrl).setHeader("Сайт");
        grid.addColumn(Site::getStatus).setHeader("Статус");
        grid.addColumn(Site::getPageCount).setHeader("Страницы");

        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);

        //Создание кнопок управления
        HorizontalLayout hLayout = createButton();
        layout.add(hLayout);

        List<Site> sites = siteRepository.findAll();
        grid.setItems(sites);
        layout.add(grid);

        siteComponent = layout;
        return layout;
    }

    private VerticalLayout getModifyComponent(int idSite) {
        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);

        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        TextField urlText = new TextField("Редактирование:");
        if (idSite == 0) {
            urlText.setLabel("Добавить сайт:");
        }

        Button saveButton = new Button("Сохранить");
        Button cancelButton = new Button("Отменить");


        layout.setMaxWidth(700, Unit.PIXELS);

        layout.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        urlText.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        saveButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        cancelButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");


        urlText.setMinWidth(190, Unit.PIXELS);
        hLayout.add(saveButton, cancelButton);

        //Все элементы на форму
        layout.add(urlText, hLayout);

        fillModifyComponent(idSite, urlText, saveButton, cancelButton);

        return layout;
    }

    private void fillModifyComponent(int id, TextField textField, Button saveButton, Button cancelButton) {
        if (!(id == 0)) {
            Optional<Site> site = siteRepository.findById(id);
            site.ifPresent(x -> {
                textField.setValue(x.getUrl());
            });
        }
        saveButton.addClickListener(clickEvent -> {
            //Создадим объект Site получив значение id
            if (!textField.getValue().equals("")) {
                Site site = new Site();
                if (!(id == 0)) {
                    site.setId(id);
                }
                String newUrl = textField.getValue();

                if (newUrl.charAt(newUrl.length() - 1) == '/') newUrl = newUrl.substring(0, newUrl.length() - 1);

                site.setUrl(newUrl);
                siteRepository.save(site);

                Notification notification = new Notification((id == 0) ? "Сайт успешно добавлен" : "Изменения внесены", 1000);
                notification.setPosition(Notification.Position.MIDDLE);
                grid.setItems(siteRepository.findAll());
                notification.addDetachListener(detachEvent -> {
                    //UI.getCurrent().navigate(MainView.class);
                    //setContent(getSitesComponent());
                    //setContent(getGridWithEditor());
                    setContent(getSimpleGrid());
                });
                //formLayout.setEnabled(false);
                notification.open();
            } else {
                showMessage("Вы ничего не внесли...", 1000, Notification.Position.MIDDLE);
                //setContent(getSitesComponent());
                //setContent(getGridWithEditor());
                setContent(getSimpleGrid());
            }
        });

        cancelButton.addClickListener(clckEvent -> {
            //setContent(getSimpleGrid());
            setContent(getSiteComponent());
            gridRefresh();
        });
    }

    public static void showMessage(String text, int duration, Notification.Position position) {
        Notification notification = new Notification(text, duration);
        notification.setPosition(position);
        notification.open();
    }

}

