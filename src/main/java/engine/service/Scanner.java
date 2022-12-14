package engine.service;

import com.vaadin.flow.component.notification.Notification;
import engine.entity.Config;
import engine.entity.Page;
import engine.entity.Site;
import engine.repository.*;
import engine.views.CreateUI;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;


public class Scanner {
    private Site site;
    private String path;
    private String content;

    private ForkJoinPool pool;
    private ConcurrentSkipListSet<String> readyLinks;
    private ConcurrentSkipListSet<Page> pages;
    private Lemmatization lemmatizator;
    private ConcurrentSkipListSet<String> inProcessLinks;
    private ConcurrentSkipListSet<String> errorLinks;

    private Integer batchSize;
    private Boolean stopScan;
    private Boolean checkPartOfSpeech;

    private static JdbcTemplate jdbcTemplate;
    private static PageRepository pageRepository;
    private static SiteRepository siteRepository;
    private static ConfigRepository configRepository;
    private static PartOfSpeechRepository partOfSpeechRepository;
    private static FieldRepository fieldRepository;


    public static void setDataAccess(ConfigRepository configRepository,
                                     SiteRepository siteRepository,
                                     PageRepository pageRepository,
                                     PartOfSpeechRepository partOfSpeechRepository,
                                     FieldRepository fieldRepository,
                                     JdbcTemplate jdbcTemplate) {
        Scanner.configRepository = configRepository;
        Scanner.siteRepository = siteRepository;
        Scanner.pageRepository = pageRepository;
        Scanner.partOfSpeechRepository = partOfSpeechRepository;
        Scanner.fieldRepository = fieldRepository;
        Scanner.jdbcTemplate = jdbcTemplate;
    }

    public Scanner(Site site, String path) {
        this.path = path;
        this.site = site;

        int siteId = site.getId();

        int parallelism = Runtime.getRuntime().availableProcessors();
        Config config = configRepository.findByKey("tps");
        try {
            if (!(config == null))
                parallelism = Integer.parseInt(config.getValue());
        } catch (Exception e) {
            CreateUI.showMessage("Тип свойства 'tps' должен быть Integer",
                    2000, Notification.Position.MIDDLE);
        }

        config = configRepository.findByKey("batch");
        try {
            if (!(config == null))
                batchSize = Integer.parseInt(config.getValue());
        } catch (Exception e) {
            CreateUI.showMessage("Тип свойства 'batch' должен быть Integer",
                    2000, Notification.Position.MIDDLE);
        }

        config = configRepository.findByKey("isPoS");
        try {
            if (!(config == null))
                checkPartOfSpeech = Boolean.parseBoolean(config.getValue());
        } catch (Exception e) {
            CreateUI.showMessage("Тип свойства 'isPoS' должен быть true/false!",
                    2000, Notification.Position.MIDDLE);
        }

        pool = new ForkJoinPool(parallelism,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null, true);

        readyLinks = new ConcurrentSkipListSet<>();
        inProcessLinks = new ConcurrentSkipListSet<>();
        pages = new ConcurrentSkipListSet<>();
        errorLinks = new ConcurrentSkipListSet<>();

        List<String> excludeList = null;
        if (checkPartOfSpeech)
            excludeList = partOfSpeechRepository.findByInclude(false)
                    .stream()
                    .map(p -> p.getShortName())
                    .collect(Collectors.toList());
        lemmatizator = new Lemmatization(excludeList, fieldRepository.findByActive(true));

    }
}
