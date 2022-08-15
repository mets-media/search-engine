package engine.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import engine.entity.Site;
import engine.service.Parser;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route
@Getter
public class MainView extends AppLayout {
    @Autowired
    SiteRepository siteRepository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    Parser parser;

    public MainView() {

        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("Search Engine");
        title.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.getStyle()
                .set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        Tab tab = new Tab("Список сайтов");
        tab.getElement().addEventListener("click", domEvent -> setContent(getSitesComponent()));
        tabs.add(tab);

//        tab = new Tab("Результаты");
//        tab.getElement().addEventListener("click", domEvent -> UI.getCurrent().navigate(SiteList.class));
//        tabs.add(tab);

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }

    public void fillGrid(Grid<Site> grid) {

        List<Site> sites = siteRepository.findAll();

        if (!sites.isEmpty()) {
            grid.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("margin", "0");

            //Cтолбцы в нужном порядке
            //grid.addColumn(Site::getUrl).setHeader("Сайт");
            grid.addColumn(Site::getUrl);

            //Кнопка Запуск парсинга
            grid.addColumn(new NativeButtonRenderer<Site>("Парсинг", site -> {
                //Вариант 1
//                Parser1 parser = new Parser1(site.getUrl(), pageRepository);
//                parser.run();
                if (!Parser.isActive()) {
                    Parser.setPageRepository(pageRepository);
                    Parser.start(site.getUrl());
                }
                Parser.setActive(false);

            })).setAutoWidth(true).setFlexGrow(0);

            //Кнопка редактирования
            grid.addColumn(new NativeButtonRenderer<Site>("Изменить", site -> {
                UI.getCurrent().navigate(ManageSite.class, site.getId());
            })).setAutoWidth(true).setFlexGrow(0);

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

    private VerticalLayout getSitesComponent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);
        //layout.setMaxWidth(600, Unit.PIXELS);

        Grid<Site> grid = new Grid<>();
        RouterLink linkCreate = new RouterLink("Добавить сайт", ManageSite.class, 0l);
        linkCreate.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "0");
        layout.add(linkCreate);
        layout.add(grid);
        fillGrid(grid);
        return layout;
    }

}

