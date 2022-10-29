package engine.views;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.router.Route;
import engine.auxEntity.AuxData;
import engine.auxRepository.MainGridRepository;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.grid.GridBufferedInlineEditor;
import engine.service.Parser;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Route
@Getter
public class MainView extends AppLayout {

    private static Grid<Site> grid = new Grid<>(Site.class, false);
    String selectedSite = "";
    @Autowired
    SiteRepository siteRepository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    Parser parser;

    @Autowired
    MainGridRepository mainGridRepository;

    VerticalLayout siteComponent;

    GridBufferedInlineEditor eGrid = null;

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
        tab.getElement().addEventListener("click", domEvent -> setContent(getSimpleGrid()));
        tabs.add(tab);

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }

    public static void gridRefresh() {
        grid.getDataProvider().refreshAll();
    }

    public static void gridRefreshSite(int siteId) {
        //Site site = siteRepository.getById(siteId);
        //grid.getDataProvider().refreshItem(site);
    }

    private VerticalLayout getSimpleGrid() {
        if (!(siteComponent == null)) {
            return siteComponent;
        }

        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        grid.addItemClickListener(event -> {
            //selectedSite = event.getItem().getUrl();
            showMessage("Сайт: " + event.getItem().getUrl(), 3000, Notification.Position.MIDDLE);
            //grid.getEditor().editItem(event.getItem());

        });

        grid.addColumn(Site::getUrl).setHeader("Сайт");
        grid.addColumn(Site::getStatus).setHeader("Статус");
        grid.addColumn(Site::getPageCount).setHeader("Страницы");

        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);

        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        layout.add(hLayout);

        Button createButton = new Button("Добавить сайт");
        createButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        createButton.addClickListener(buttonClickEvent -> {
            setContent(getModifyComponent(0));

        });
        hLayout.add(createButton);


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
        hLayout.add(deleteButton);

        Button parseButton = new Button("Сканировать");
        parseButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        parseButton.addClickListener(buttonClickEvent -> {
            Parser.setPageRepository(pageRepository);
            Optional<Site> currentSite = grid.getSelectedItems().stream().findFirst();
            // TODO: 27.10.2022  Реализовать работу по нескольким сайтам

            //Запуск в отдельном потоке
            Runnable parse = () -> {
                Site site = currentSite.get();
                site.setStatus(SiteStatus.DOWNLOADING);
                siteRepository.save(site);
                int siteId = site.getId();
                Parser.start(siteId, currentSite.get().getUrl());
            };
            Thread thread = new Thread(parse);
            thread.start();

            grid.getDataProvider().refreshAll();

        });
        hLayout.add(parseButton);

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

