package engine.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

import com.vaadin.flow.router.Route;

import engine.repository.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.HashMap;
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
    @Autowired
    PartOfSpeechRepository partOfSpeechRepository;

    @Autowired
    private ApplicationContext context;

    @PersistenceContext
    EntityManager entityManager;

    private SiteComponent siteComponent;
    private HashMap<String, VerticalLayout> contentsHashMap = new HashMap<>();

    @PostConstruct
    private void getSites() {
        SiteComponent.setDataAccess(configRepository,
                siteRepository,
                pageRepository,
                fieldRepository,
                partOfSpeechRepository,
                jdbcTemplate,
                entityManager);
        siteComponent.getGrid().setItems(siteRepository.findAll());
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
        Tab tabLemma = new Tab("Поиск");
        Tab tabIndexing = new Tab("Индексация");

        tabs.addSelectedChangeListener(event -> {
            String label = tabs.getSelectedTab().getLabel();

            switch (label) {
                case "Сайты" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        SiteComponent.setDataAccess(configRepository,
                                siteRepository,
                                pageRepository,
                                fieldRepository,
                                partOfSpeechRepository,
                                jdbcTemplate,
                                entityManager);
                        siteComponent = new SiteComponent();
                        setContent(siteComponent.getMainLayout());
                        //siteComponent.getGrid().setItems(siteRepository.findAll());
                        contentsHashMap.put(label, siteComponent.getMainLayout());
                    } else {
                        //siteComponent.getGrid().setItems(siteRepository.findAll());

                    }
                }
                case "Настройки" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        ConfigComponent.setConfigRepository(configRepository);
                        ConfigComponent configComponent = new ConfigComponent();
                        setContent(configComponent.getMainLayout());
                        configComponent.getGrid().setItems(configRepository.findAll());
                        contentsHashMap.put(label, configComponent.getMainLayout());

//                        Arrays.stream(context.getBeanDefinitionNames()).sorted()
//                                .collect(Collectors.toList())
//                                .forEach(System.out::println);
                    }
                }
                case "Поиск" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        LemmaComponent.setPartOfSpeechRepository(partOfSpeechRepository);
                        LemmaComponent lemmaComponent = new LemmaComponent();
                        setContent(lemmaComponent.getMainLayout());
                        contentsHashMap.put(label, lemmaComponent.getMainLayout());
                    }
                }
                case "Индексация" -> {
                    if (!contentsHashMap.containsKey(label)) {
                        IndexingComponent.dataAccess(fieldRepository,
                                pageRepository,
                                siteRepository,
                                partOfSpeechRepository,
                                entityManager);
                        IndexingComponent indexingComponent = new IndexingComponent();
                        setContent(indexingComponent.getMainLayout());
                        contentsHashMap.put(label, indexingComponent.getMainLayout());
                    }
                }
            }
            setContent(contentsHashMap.get(label));
        });

        tabs.add(tabSites, tabOptions, tabIndexing, tabLemma);

        addToDrawer(tabs);
        addToNavbar(toggle, title);
    }
}

