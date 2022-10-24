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
import engine.grid.GridBufferedInlineEditor;
import engine.service.Parser;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@Route
@Getter
public class MainView extends AppLayout {

    String selectedSite = "";
    @Autowired
    SiteRepository siteRepository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    Parser parser;

    @Autowired
    MainGridRepository mainGridRepository;

    GridBufferedInlineEditor eGrid = null;

    public MainView() {

        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("Search Engine");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");

//        Tab tab = new Tab("Список сайтов");
//        tab.getElement().addEventListener("click", domEvent -> setContent(getSitesComponent()));
//        tabs.add(tab);

        Tab tab = new Tab("Анализ сайтов");
        tab.getElement().addEventListener("click", domEvent -> setContent(getGridWithEditor()));
        tabs.add(tab);

        tab = new Tab("Simple Grid");
        tab.getElement().addEventListener("click", domEvent -> setContent(getSimpleGrid()));
        tabs.add(tab);

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }

    public void fillGrid(Grid<Site> grid) {

        List<Site> sites = siteRepository.findAll();

        if (!sites.isEmpty()) {
            //grid.setSelectionMode(Grid.SelectionMode.NONE);
            grid.setSelectionMode(Grid.SelectionMode.SINGLE);

            grid.addItemClickListener(
                    event -> {
                        //showMessage("Clicked Item: " + event.getItem(), 1000, Notification.Position.MIDDLE);
                        grid.getEditor().editItem(event.getItem());
                    });

            grid.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxs)")
                    .set("margin", "0");


            //Cтолбцы в нужном порядке
            //grid.addColumn(Site::getUrl).setHeader("Сайт");
            grid.addColumn(Site::getUrl);

            //Кнопка Запуск парсинга
            grid.addColumn(new NativeButtonRenderer<Site>("Парсинг", site -> {
                if (!Parser.isActive()) {

                    //Parser.setPageRepository(pageRepository);
                    //0L - заглушка
                    Parser.start(0L, site.getUrl());
                }
                Parser.setActive(false);

            })).setAutoWidth(true).setFlexGrow(0);

            //Кнопка редактирования
            grid.addColumn(new NativeButtonRenderer<Site>("Изменить", site -> {
                //notifyShow("Id: " + grid.getSelectedItems().stream().findFirst().get().getId(), 2000);
                //Возможно при MULTISELECT в Grid
                //setContent(getModifyComponent(grid.getSelectedItems().stream().findFirst().get().getId()));
                setContent(getModifyComponent(site.getId()));
            })).setAutoWidth(true).setFlexGrow(0);

            //Кнопка редактирования первый вариант
//            grid.addColumn(new NativeButtonRenderer<Site>("Изменить", site -> {
//                UI.getCurrent().navigate(ManageSite.class, site.getId());
//            })).setAutoWidth(true).setFlexGrow(0);

            Button createButton = new Button("Добавить сайт");
            createButton.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("margin", "0");

            createButton.addClickListener(buttonClickEvent -> {
                setContent(getModifyComponent(0l));
            });

            //Кнопка удаления
            grid.addColumn(new NativeButtonRenderer<Site>("Удалить", site -> {
                Dialog dialog = new Dialog();
                Button confirm = new Button("Удалить");
                Button cancel = new Button("Отмена");

                dialog.add("Вы уверены что хотите удалить сайт?");
                dialog.add(confirm);
                dialog.add(cancel);
                confirm.addClickListener(clickEvent -> {
                    siteRepository.delete(site);
                    dialog.close();
                    Notification notification = new Notification("Сайт удален", 1000);
                    notification.setPosition(Notification.Position.MIDDLE);
                    notification.open();
                    grid.setItems(siteRepository.findAll());
                });
                cancel.addClickListener(clickEvent -> {
                    dialog.close();
                });
                dialog.open();
            })).setAutoWidth(true).setFlexGrow(0); //.setWidth("8em").setFlexGrow(0);;
            grid.setItems(sites);
        }
    }

    private VerticalLayout getGridWithEditor() {

//        if (eGrid == null) {
//
//        }

        //Данные о структуре
        AuxData.fillStructure(mainGridRepository);

        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);

        Button createButton = new Button("Добавить сайт");
        createButton.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");


        createButton.addClickListener(buttonClickEvent -> {
            setContent(getModifyComponent(0l));

        });
        layout.add(createButton);

        GridBufferedInlineEditor eGrid = new GridBufferedInlineEditor(mainGridRepository);
        eGrid.addColumns(mainGridRepository.findAll());

        List<Site> sites = siteRepository.findAll();
        eGrid.getGrid().setItems(sites);

        layout.add(eGrid.getGrid());

        return layout;
    }


    private VerticalLayout getSimpleGrid() {
        Grid<Site> grid = new Grid<>(Site.class, false);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        grid.addItemClickListener(
                event -> {
                    selectedSite = event.getItem().getUrl();
                    showMessage("Сайт: " + event.getItem().getUrl(), 3000, Notification.Position.MIDDLE);
                    //grid.getEditor().editItem(event.getItem());
                });

        grid.addColumn(Site::getUrl).setHeader("Сайт");
        //grid.addColumn(Site::getPageCount);


        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);

        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        layout.add(hLayout);

        Button createButton = new Button("Добавить сайт");
        createButton.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");
        createButton.addClickListener(buttonClickEvent -> {
            setContent(getModifyComponent(0l));

        });
        hLayout.add(createButton);


        //=================  Кнопка удаления Сайта  =======================
        Button deleteButton = new Button("Удалить");
        deleteButton.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");

        deleteButton.addClickListener(buttonClickEvent -> {
            Dialog dialog = new Dialog();
            Button confirm = new Button("Удалить");
            Button cancel = new Button("Отмена");

            dialog.add("Вы уверены что хотите удалить сайт?");
            dialog.add(confirm);
            dialog.add(cancel);
            confirm.addClickListener(clickEvent -> {

                Optional<Site> site = grid.getSelectedItems().stream().findFirst();
                siteRepository.delete(site.get());

                dialog.close();
                Notification notification = new Notification("Сайт удален", 1000);
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
        parseButton.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");

        parseButton.addClickListener(buttonClickEvent -> {
            Parser.setPageRepository(pageRepository);
            Optional<Site> currentSite = grid.getSelectedItems().stream().findFirst();

            //Запуск сканирования
            Parser.start(currentSite.get().getId(), currentSite.get().getUrl());
        });
        hLayout.add(parseButton);

        List<Site> sites = siteRepository.findAll();
        grid.setItems(sites);
        layout.add(grid);

        return layout;
    }


    private VerticalLayout getSitesComponent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);
        //layout.setMaxWidth(600, Unit.PIXELS);

        Grid<Site> grid = new Grid<>();

        //Первый вариант...
//        RouterLink linkCreate = new RouterLink("Добавить сайт", ManageSite.class, 0l);
//        linkCreate.getStyle()
//                .set("font-size", "var(--lumo-font-size-xs)")
//                .set("margin", "0");
//        layout.add(linkCreate);

        Button createButton = new Button("Добавить сайт");
        createButton.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "0");

        createButton.addClickListener(buttonClickEvent -> {
            setContent(getModifyComponent(0l));
        });
        layout.add(createButton);

        layout.add(grid);
        fillGrid(grid);
        return layout;
    }

    private VerticalLayout getModifyComponent(Long idSite) {
        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);

        TextField urlText = new TextField("Редактирование:");
        if (idSite == 0) {
            urlText.setLabel("Добавить сайт:");
        }

        Button saveButton = new Button("Сохранить");


        layout.setMaxWidth(700, Unit.PIXELS);

        layout.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");
        urlText.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");
        saveButton.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("margin", "0");

        //Все элементы на форму
        layout.add(urlText, saveButton);

        fillModifyComponent(idSite, urlText, saveButton);

        return layout;
    }

    private void fillModifyComponent(Long id, TextField textField, Button button) {
        if (!id.equals(0)) {
            Optional<Site> site = siteRepository.findById(id);
            site.ifPresent(x -> {
                textField.setValue(x.getUrl());
            });
        }
        button.addClickListener(clickEvent -> {
            //Создадим объект Site получив значение id
            if (!textField.getValue().equals("")) {
                Site site = new Site();
                if (!id.equals(0l)) {
                    site.setId(id);
                }
                site.setUrl(textField.getValue());
                siteRepository.save(site);

                Notification notification = new Notification(id.equals(0l) ? "Сайт успешно добавлен" : "Изменения внесены", 1000);
                notification.setPosition(Notification.Position.MIDDLE);
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
    }

    private void showMessage(String text, int duration, Notification.Position position) {
        Notification notification = new Notification(text, duration);
        notification.setPosition(position);
        notification.open();
    }

}

