package engine.views;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;

import com.vaadin.flow.router.Route;

import engine.entity.*;
import engine.repository.ConfigRepository;
import engine.repository.FieldRepository;
import engine.service.Parser;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Route
@Getter
public class MainView extends AppLayout {
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    ConfigRepository configRepository;

    @Autowired
    FieldRepository fieldRepository;

    public MainView() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("Search Engine");
        title.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        Tab tabSites = new Tab("Сайты");
        tabSites.getElement().addEventListener("click", domEvent -> {
            SiteComponent.setDataAccess(configRepository,siteRepository, pageRepository, fieldRepository, jdbcTemplate);
            SiteComponent siteComponent = new SiteComponent();
            setContent(siteComponent.getVerticalLayout());
            siteComponent.getGrid().setItems(siteRepository.findAll());
        });

        Tab tabOptions = new Tab("Настройки");
        tabOptions.getElement().addEventListener("click", domEvent -> {
            ConfigComponent.setConfigRepository(configRepository);
            ConfigComponent configComponent = new ConfigComponent();
            setContent(configComponent.getVerticalLayout());
            configComponent.getGrid().setItems(configRepository.findAll());
        });

        Tab tabLemma = new Tab("Лемматизатор");
        tabLemma.getElement().addEventListener("click", domEvent -> {
            LemmaComponent lemmaComponent = new LemmaComponent();
            setContent(lemmaComponent.getVerticalLayout());
        });

        tabs.add(tabSites, tabOptions, tabLemma);

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }



}

