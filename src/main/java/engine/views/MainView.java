package engine.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

import com.vaadin.flow.router.Route;

import engine.repository.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
    @Autowired
    PartOfSpeechRepository partOfSpeechRepository;

    @PersistenceContext
    EntityManager entityManager;

    public MainView() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("Search Engine");
        title.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        Tab tabSites = new Tab("Сайты");
        tabSites.getElement().addEventListener("click", domEvent -> {
            SiteComponent.setDataAccess(configRepository,siteRepository, pageRepository,fieldRepository, jdbcTemplate);
            SiteComponent siteComponent = new SiteComponent();
            setContent(siteComponent.getMainLayout());
            siteComponent.getGrid().setItems(siteRepository.findAll());
        });

        Tab tabOptions = new Tab("Настройки");
        tabOptions.getElement().addEventListener("click", domEvent -> {
//            if (configRepository.count()==0)
//                configRepository.initData();

            ConfigComponent.setConfigRepository(configRepository);
            ConfigComponent configComponent = new ConfigComponent();
            setContent(configComponent.getMainLayout());
            configComponent.getGrid().setItems(configRepository.findAll());
        });

        Tab tabLemma = new Tab("Лемматизатор");
        tabLemma.getElement().addEventListener("click", domEvent -> {
            LemmaComponent lemmaComponent = new LemmaComponent();
            lemmaComponent.setPartOfSpeechRepository(partOfSpeechRepository);
            setContent(lemmaComponent.getMainLayout());
        });

        Tab tabIndexing = new Tab("Индексация");
        tabIndexing.getElement().addEventListener("click", domEvent -> {
            IndexingComponent indexingComponent = new IndexingComponent();
            indexingComponent.dataAccess(fieldRepository,
                    pageRepository,
                    siteRepository,
                    entityManager);
            setContent(indexingComponent.getMainLayout());
        });

        tabs.add(tabSites, tabOptions, tabLemma, tabIndexing);

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }



}

