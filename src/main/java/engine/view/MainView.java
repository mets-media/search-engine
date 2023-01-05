package engine.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

import com.vaadin.flow.router.Route;

import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import engine.config.YAMLConfig;
import engine.entity.SiteStatus;
import engine.repository.*;
import engine.service.BeanAccess;
import engine.service.Parser;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.HashMap;

@Route
@Getter
public class MainView extends AppLayout {
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    ConfigRepository configRepository;
    @Autowired
    FieldRepository fieldRepository;
    @Autowired
    PartOfSpeechRepository partOfSpeechRepository;
    @Autowired
    PathTableRepository pathTableRepository;
    @Autowired
    PageContainerRepository pageContainerRepository;
    @Autowired
    StatusRepository statusRepository;
    @Autowired
    YAMLConfig yamlConfig;
    @PersistenceContext
    EntityManager entityManager;
    @Autowired
    BeanAccess beanAccess;

    private final HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();

    @PostConstruct
    private void getSites() {
        beanAccess.setUi(UI.getCurrent());
        SiteComponent.setDataAccess(beanAccess);

        var listSites = yamlConfig.getSites();

        if (!(listSites == null))
            listSites.forEach(site -> {
                if (!siteRepository.getSiteByUrl(site.getUrl()).isPresent()) {
                    site.setPageCount(0);
                    site.setStatus(SiteStatus.NEW_SITE);
                    site.setStatusTime(LocalDateTime.now());
                    siteRepository.save(site);
                }
            });

        yamlConfig.setSites(null);

        var siteComponent = new SiteComponent();
        setContent(siteComponent.getMainLayout());
        contentsHashMap.put("Сайты", siteComponent.getMainLayout());
        siteComponent.getGrid().setItems(siteRepository.findAll());

        if (yamlConfig.getAutoScan()) {
            Parser.setDataAccess(
                    siteComponent.getGrid(),
                    beanAccess);

            listSites.forEach(site -> {
                Parser.getStopList().remove(site);
                site.setStatus(SiteStatus.INDEXING);
                siteRepository.save(site);
                Parser.start(site);
            });
        }
        //beanAccess.setUi(UI.getCurrent());
    }

    public MainView() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("Search Engine");
        title.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        Tab tabSites = new Tab("Сайты");
        Tab tabOptions = new Tab("Настройки");
        Tab tabLemma = new Tab("Лемматизатор");
        Tab tabIndexing = new Tab("Индексация");
        Tab tabSearch = new Tab("Поиск");

        tabs.addSelectedChangeListener(event -> {
            String label = tabs.getSelectedTab().getLabel();

            switch (label) {
//                case "Сайты" -> {
//                    if (!contentsHashMap.containsKey(label)) {
//                    }
//                }
                case "Настройки" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        ConfigComponent.setConfigRepository(configRepository);
                        ConfigComponent configComponent = new ConfigComponent();
                        setContent(configComponent.getMainLayout());
                        configComponent.getGrid().setItems(configRepository.findAll());
                        contentsHashMap.put(label, configComponent.getMainLayout());
                    }
                }
                case "Лемматизатор" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        //LemmaComponent.setPartOfSpeechRepository(partOfSpeechRepository);
                        LemmaComponent lemmaComponent = new LemmaComponent(beanAccess);
                        setContent(lemmaComponent.getMainLayout());
                        contentsHashMap.put(label, lemmaComponent.getMainLayout());
                    }
                }
                case "Индексация" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        IndexingComponent.setDataAccess(beanAccess);
                        IndexingComponent indexingComponent = new IndexingComponent();
                        setContent(indexingComponent.getMainLayout());
                        contentsHashMap.put(label, indexingComponent.getMainLayout());
                    }

                }
                case "Поиск" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        SearchComponent.setDataAccess(beanAccess);
                        SearchComponent searchComponent = new SearchComponent();
                        setContent(searchComponent.getMainLayout());
                        contentsHashMap.put(label, searchComponent.getMainLayout());
                    }
                }
            }
            setContent(contentsHashMap.get(label));
        });

        tabs.add(tabSites, tabOptions, tabIndexing, tabLemma, tabSearch);

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }

}

