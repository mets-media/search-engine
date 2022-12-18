package engine.service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.Theme;
import engine.entity.*;
import engine.repository.*;
import engine.views.CreateUI;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.InterruptibleBatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

//@Component
@Service
@RequiredArgsConstructor
public class Parser extends RecursiveAction {
    private Site site;
    private String path;
    private String content;
    private Integer code;
    private String domainName;
    private static Grid<Site> siteGrid;
    private static BeanAccess beanAccess;
    public final static String READY_LINKS_FILENAME = "links.txt";
    public final static String STOP_LINKS_FILENAME = "Stoplinks.txt";
    public final static String ERROR_LINKS_FILENAME = "Error_Links.txt";
    private static final HashMap<Integer, ForkJoinPool> activePools = new HashMap<>();
    private static final ConcurrentHashMap<Integer, ConcurrentSkipListSet<String>> readyLinksHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, List<Page>> pageHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Lemmatization> lemmatizatorHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ConcurrentSkipListSet<String>> inProcessLinksHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ConcurrentSkipListSet<String>> errorLinksHashMap = new ConcurrentHashMap<>();
    private static Integer batchSize = 100;

    private static Boolean checkPartOfSpeech = true;

    private static final boolean saveLinksInShortFormat = false;

    private static final HashSet<Integer> stopList = new HashSet<>();
    private static final AtomicLong totalCountLinks = new AtomicLong();
    private static AtomicBoolean writeInProgress = new AtomicBoolean();
    public static Set<Integer> getStopList() {
        return stopList;
    }
    public static void setDataAccess(Grid<Site> siteGrid,
                                     BeanAccess beanAccess) {
        Parser.siteGrid = siteGrid;
        Parser.beanAccess = beanAccess;
    }

    public Parser(Site site, String path, String domainName) {
        this.path = path;
        this.domainName = domainName;
        this.site = site;
    }

    private static void deleteFile(String fileName) {
        try {
            Files.delete(Paths.get(fileName));
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public static List<String> loadLinksFromFile(String fileName) {

        List<String> links = null;
        try {
            links = Files.readAllLines(Paths.get(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return links;
    }

    public static void saveHashMapToFile(String fileName,
                                         String domainName,
                                         ConcurrentHashMap<String, Page> linksHashMap,
                                         Boolean shortFormat,
                                         SaveFileMode saveFileMode) {
        Path path = Paths.get(fileName);
        switch (saveFileMode) {
            case REWRITE -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
            case DO_NOT_REWRITE -> {
                if (Files.exists(path)) {
                    return;
                }
            }
        }

        Set<String> linkContent = new HashSet<>();

        if (!shortFormat)
            linksHashMap.entrySet().forEach(lC -> {
                Page page = lC.getValue();
                linkContent.add(page.getSiteId().toString().concat("\t")
                        .concat(page.getPath()).concat("\t")
                        .concat(page.getCode().toString()).concat("\t")
                        .concat(page.getContent()));
            });
        else {
            linksHashMap.entrySet().forEach(lC -> {
                Page page = lC.getValue();
                linkContent.add(page.getSiteId().toString().concat("\t")
                        .concat(HtmlParsing.getShortLink(page.getPath(), domainName)).concat("\t")
                        .concat(page.getCode().toString()).concat("\t")
                        .concat(page.getContent()));
            });
        }
        try {
            Files.write(path, linkContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveLinksToFile(String fileName,
                                       String domainName,
                                       //List<String> links,
                                       ConcurrentSkipListSet<String> links,
                                       Boolean shortFormat,
                                       SaveFileMode saveFileMode) {

        Path path = Paths.get(fileName);

        switch (saveFileMode) {
            case REWRITE -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
            case APPEND -> {
            }
            case DO_NOT_REWRITE -> {
                if (Files.exists(path)) {
                    return;
                }
            }
        }

        if (!shortFormat) {
            try {
                Files.write(path, links);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        Set<String> shortLinks = new HashSet<>();


        links.forEach(l -> {
            String link = l.toString();

            String shortLink = link.substring(link.indexOf(domainName) + domainName.length());
            if ("".equals(shortLink))
                if (link.contains("//".concat(domainName)))
                    shortLink = "/";
                else
                    shortLink = link;

            if ("/".equals(shortLink)) {
                if (link.contains(".".concat(domainName)))
                    shortLink = link;
            }
            shortLinks.add(shortLink);
        });

        try {
            Files.write(path, shortLinks);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void initSite(Site site, String readyLinksFilename, String stopLinksFilename) {

        int siteId = site.getId();

        int parallelism = Runtime.getRuntime().availableProcessors();
        Config config = beanAccess.getConfigRepository().findByKey("tps");
        try {
            if (!(config == null))
                parallelism = Integer.parseInt(config.getValue());
        } catch (Exception e) {
            CreateUI.showMessage("Тип свойства 'tps' должен быть Integer",
                    2000, Notification.Position.MIDDLE);
        }

        config = beanAccess.getConfigRepository().findByKey("batch");
        try {
            if (!(config == null))
                batchSize = Integer.parseInt(config.getValue());
        } catch (Exception e) {
            CreateUI.showMessage("Тип свойства 'batch' должен быть Integer",
                    2000, Notification.Position.MIDDLE);
        }

        config = beanAccess.getConfigRepository().findByKey("isPoS");
        try {
            if (!(config == null))
                checkPartOfSpeech = Boolean.parseBoolean(config.getValue());
        } catch (Exception e) {
            CreateUI.showMessage("Тип свойства 'isPoS' должен быть true/false!",
                    2000, Notification.Position.MIDDLE);
        }

        activePools.put(siteId, new ForkJoinPool(parallelism,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null, true));
        inProcessLinksHashMap.put(siteId, new ConcurrentSkipListSet<>());
        //readyLinksHashMap.put(siteId, new ConcurrentHashMap<>());
        readyLinksHashMap.put(siteId, new ConcurrentSkipListSet<>());
        //pageHashMap.put(siteId, new ArrayList<>());
        pageHashMap.put(siteId, new ArrayList<>());
        errorLinksHashMap.put(siteId, new ConcurrentSkipListSet<>());

        List<String> excludeList = null;
        if (checkPartOfSpeech)
            excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                    .stream()
                    .map(PartsOfSpeech::getShortName)
                    .collect(Collectors.toList());
        Lemmatization lemmatizator = new Lemmatization(excludeList, beanAccess.getFieldRepository().findByActive(true));

        lemmatizatorHashMap.put(siteId, lemmatizator);


        if (Files.exists(Paths.get(readyLinksFilename))) {
            var rLinks = new ConcurrentSkipListSet<String>(loadLinksFromFile(readyLinksFilename));
            readyLinksHashMap.get(site.getId()).addAll(rLinks);
        }

        Path path = Paths.get(stopLinksFilename);
        if (Files.exists(path)) {
            inProcessLinksHashMap.get(siteId).addAll(loadLinksFromFile(stopLinksFilename));

            try {//необходимо удалить файл поскольку применяетя DO_NOT_REWRITE
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void start(Site site) {
        String path = site.getUrl();
        String domainName = HtmlParsing.getDomainName(path);

        String readyLinksFilename = "data/" + domainName + "/" + READY_LINKS_FILENAME;
        String stopLinksFilename = "data/" + domainName + "/" + STOP_LINKS_FILENAME;

        //Инициализация статических переменных для класса Parser
        initSite(site, readyLinksFilename, stopLinksFilename);
        //ConcurrentHashMap<String, Page> readyLinks = readyLinksHashMap.get(site.getId());
        var readyLinks = readyLinksHashMap.get(site.getId());

        int pageCount = beanAccess.getPageRepository().countBySiteId(site.getId());

        if (pageCount > 0) // Если существуют страницы в базе данных
            showPrevDataDialog(site, pageCount);

//        try {
//            Files.createDirectories(Paths.get("data/" + domainName));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        TimeMeasure.setStartTime();
        stopList.remove(site.getId());
        writeInProgress.set(false);

        Parser parser;
        if (readyLinks.size() == 0) {
            parser = new Parser(site, path, domainName);
            activePools.get(site.getId()).execute(parser);
            System.out.printf("Старт для сайта: %s\n", site.getUrl());
            return;
        }

        ConcurrentSkipListSet<String> stopLinks = inProcessLinksHashMap.get(site.getId());
        if (!(stopLinks.size() == 0))
            stopLinks.forEach(p -> {
                System.out.println("Перезапуск: " + p);
                activePools.get(site.getId()).execute(new Parser(site, p, domainName));
                stopLinks.remove(p);
            });

        System.out.printf("Потоки запущены для сайта: %s\n", site.getUrl());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public static void writeToKeepLink(Integer siteId, List<String> links) {
        System.out.println("Записываю: " + links.size());
        String sql = "Insert into Keep_Link (Site_Id, Path) values (?,?)";

        int[] result = beanAccess.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String link = links.get(i);
                ps.setInt(1, siteId);
                ps.setString(2, link);
            }

            @Override
            public int getBatchSize() {
                return links.size();
            }
        });

        //links.clear();
        //for (int i = 0; i < result.length; i++) System.out.println(result[i]);
    }

    private static void insertIntoPageContainer(Integer siteId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Insert into Page_Container (Site_Id, Code, Path, Content, Lemmatization) values ");
        //stringBuilder.append("Insert into Page_Container (Site_Id, Code, Path, Content, Lemmatization) values (-1,-1,'path','content','Lemmatization')");

        var readyPages = pageHashMap.get(siteId);
        List<Page> pages = new ArrayList<>(readyPages);
        for (Page page : pages) {
            String content = page.getContent();
            String lemmaString = "'" + getLemmaString(content, lemmatizatorHashMap.get(siteId)) + "'";
            lemmaString = "___";

            stringBuilder.append("(".concat(page.getSiteId().toString()).concat(", ")
                    .concat(page.getCode().toString()).concat(", ")
                    .concat("'" + page.getPath()).concat("', ")
                    .concat("'" + content).concat("', ")
                    .concat("'" + lemmaString).concat("'),"));
        }

        TransactionTemplate transactionTemplate = beanAccess.getTransactionTemplate();
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                beanAccess.getJdbcTemplate().execute(stringBuilder.toString());
            }
        });


    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public static Integer writeTempTable(Integer siteId) {

        if (writeInProgress.get()) {
            System.out.println("WriteInProgress");
            return 0;
        }

        System.out.println(Thread.currentThread().getName());

        writeInProgress.set(true);

        var readyPages = pageHashMap.get(siteId);
        List<Page> pages = new ArrayList<>(readyPages);

        List<String> lemmaStrings = new ArrayList<>();
        for (Page page: pages) {
            lemmaStrings.add(getLemmaString(page.getContent(), lemmatizatorHashMap.get(siteId)));
        }

        //int pageCountBefore = pageRepository.countBySiteId(siteId);

        System.out.println("Записываю: " + pages.size());
        String sql = "Insert into Page_Container (Site_Id, Code, Path, Content, Lemmatization) values (?,?,?,?,?)";

        //int[] result = beanAccess.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
        int[] result = beanAccess.getJdbcTemplate().batchUpdate(sql, new InterruptibleBatchPreparedStatementSetter() {
            @Override
            public boolean isBatchExhausted(int i) {
                if (i<batchSize) return true;
                else return false;
            }

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
//            public void setValues(PreparedStatement ps, int i)  {
                Page page = pages.get(i);
//                try {
                Integer siteId = page.getSiteId();
                ps.setInt(1, siteId);
                Integer code = page.getCode();
                ps.setInt(2, code);
                ps.setString(3, page.getPath());
                String content = page.getContent();
                ps.setString(4, content);

                String lemmaString = getLemmaString(content, lemmatizatorHashMap.get(siteId));
                //String lemmaString = lemmaStrings.get(i);

                if (lemmaString.isEmpty())
                    System.out.println("lemmaString is empty " + page.getPath());
                if (lemmaString == null)
                    System.out.println("lemmaString is null " + page.getPath());

                ps.setString(5, lemmaString);

//                } catch (Exception e) {
//                    System.out.println("Ошибка при операции записи " + i);
//                    e.printStackTrace();
//                }
            }

            @Override
            public int getBatchSize() {
                return pages.size();
            }
        });

        String resultBatchSave = "Success!";
        for (int j : result)
            if (!(j == 1)) resultBatchSave = "Errors!";

        System.out.printf("totalCountLinks: %d, Save - %s", totalCountLinks.get(), resultBatchSave);

        readyPages.removeAll(pages);

        writeInProgress.set(false);

        return result.length;
    }

    //@Transactional(propagation = Propagation.REQUIRES_NEW)
    public static Integer parsePageContainer() {
        return beanAccess.getPageContainerRepository().parsePageContainer();
    }


    public static void stop(Site site) {
        System.out.printf("!!! Стоп сайта: %s\n", site.getUrl());

        Integer siteId = site.getId();
        if (!activePools.containsKey(siteId))
            return;
        stopList.add(siteId);
    }


    @Override
    protected void compute() {
        int siteId = site.getId();
        ConcurrentSkipListSet<String> errorLinks = errorLinksHashMap.get(siteId);

        ForkJoinPool pool = activePools.get(siteId);
        var inProcessLinks = inProcessLinksHashMap.get(siteId);
        var readyLinks = readyLinksHashMap.get(siteId);
        var readyPages = pageHashMap.get(siteId);

        if (stopList.contains(siteId)) {
            pool.shutdown();
            return;
        }
        Document document = null;

        if (readyLinks.contains(path))
            return;
        else {
            try {
                code = HtmlParsing.getStatusCode(path);
                document = HtmlParsing.getHtmlDocument(path);
                content = document.body().toString();
            } catch (Exception e) {
                //throw new RuntimeException(e);
                //e.printStackTrace();
                code = HtmlParsing.getStatusFromExceptionString(e.toString());
                String errorLinkString;
                if (code == -2) {
                    errorLinkString = "-02".concat(": ").concat(path);
                } else
                    errorLinkString = code.toString().concat(": ").concat(path);
                errorLinks.add(errorLinkString);
            }
            if (code == 200) {
                readyLinks.add(path);
                Page page = new Page(site.getId(), path, code, content);
                readyPages.add(page);

                //System.out.printf("%s -> readyLinks: %d, inProcessLinks: %d\n", domainName, readyLinks.size(), inProcessLinks.size());

                totalCountLinks.addAndGet(1L);

                //Запись в базу данных
                if (readyLinks.size() % batchSize == 0) {
                    //if ((readyLinks.size() >= batchSize) && (!writeInProgress.get())) {
                    TimeMeasure.setStartTime();

                    //statusRepository.save(new Status(siteId, SiteStatus.BATCH_SAVE,"Start (Batch save) "));
                    writeTempTable(siteId);
                    beanAccess.getStatusRepository().save(new Status(siteId, SiteStatus.BATCH_SAVE, "Success (Batch save) totalCountLinks: " + totalCountLinks));

                    //beanAccess.getStatusRepository().save(new Status(siteId, SiteStatus.SAVE_MANUAL_TRASACTION, "Старт транзакции в ручную"));
                    //insertIntoPageContainer(siteId);

//                    int pageParsing = parsePageContainer();
//                    System.out.println("Обработано страниц: " + pageParsing);

                    System.out.println(domainName + " -> время записи в базу данных: " + TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

                    int pageInBase = beanAccess.getPageRepository().countBySiteId(siteId);
                    System.out.println("Страниц после записи: " + pageInBase);

                    site.setPageCount(pageInBase);
                    beanAccess.getSiteRepository().save(site);
                }
            }
        }

        inProcessLinks.remove(path);

        //Условие остановки!
        if (pool.isShutdown())  //Pool остановлен
            if (pool.getActiveThreadCount() == 1) { //я - последний поток!
                //Записать все readyPage
                System.out.println("Запись readyPage");

                writeTempTable(siteId);

                //Записать inProcessLinks включая текущую - она последняя
                System.out.println("Запись inProcessLink");
                inProcessLinks.add(path);
                writeToKeepLink(siteId, inProcessLinks.stream().toList());


                System.out.println("Запись errorLinks");
                writeToKeepLink(siteId, errorLinksHashMap.get(siteId).stream().toList());

                System.out.println("readyPage.size: " + readyPages.size());
                System.out.println("inProccesLinks: " + inProcessLinks.size());
                System.out.println("Удаление activePools.remove(siteId)");
                activePools.remove(siteId);
            }

        //Рекурсия
        Set<String> hReference = HtmlParsing.getAllLinks(document, domainName);
        if (hReference != null)
            for (String hRef : hReference) {
                if (!inProcessLinks.contains(hRef))
                    //if ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!readyLinks.keySet().contains(hRef))) {
                    if ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!readyLinks.contains(hRef))) {
                        inProcessLinks.add(hRef);
                        Parser parser = new Parser(site, hRef, domainName);
                        pool.execute(parser);
                    }
            }

        //Проверка на окончание загрузки
        if (inProcessLinks.size() == 0) {
            if ((!(code == 200)) &&
                    (readyPages.size() == 0) &&
                    (errorLinks.size() == 1) &&
                    (errorLinks.first().substring(4).equals(path))) {
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(SiteStatus.FAILED);
                site.setLastError("Не удалось загрузить стартовую страницу");
                beanAccess.getSiteRepository().save(site);
                System.out.println("Не удалось загрузить стартовую страницу");
            } else {
                site.setStatus(SiteStatus.LOADED);
                beanAccess.getSiteRepository().save(site);
                stop(site);
                System.out.println(site.getUrl() + " -> Загрузка завершена.");
            }
            siteGrid.setItems(beanAccess.getSiteRepository().findAll());
        }
    }

    private static void showPrevDataDialog(Site site, Integer pageCount) {
        //ConcurrentHashMap<String, Page> readyLinks = readyLinksHashMap.get(site.getId());
        ConcurrentSkipListSet readyLinks = readyLinksHashMap.get(site.getId());

        Dialog dialog = new Dialog();
        dialog.setModal(true);

        Button confirm = new Button("Удалить");
        Button cancel = new Button("Продолжить");

        dialog.setHeaderTitle("Найдено " + pageCount + " страниц в базе данных");
        dialog.getFooter().add(cancel, confirm);

        VerticalLayout verticalLayout = new VerticalLayout(new Label("Удалить результаты предыдущего сканирования?"));
        dialog.add(verticalLayout);

        confirm.addClickListener(clickEvent -> {
            dialog.close();
            Notification notification = new Notification("Удалено", 500);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.open();
            new Thread() {
                public void run() {
                    beanAccess.getPageRepository().deleteBySiteId(site.getId());
                }
            }.start();


        });
        cancel.addClickListener(clickEvent -> {
            dialog.close();
            CreateUI.showMessage("Результаты сохранены", 500, Notification.Position.MIDDLE);

            List<String> links = beanAccess.getPageRepository().getLinksBySiteId(site.getId());

            String path = site.getUrl();

            //Удаление предыдущих результатов
            readyLinks.clear();

            //readyLinks.put("/", emptyPage);
            //readyLinks.put(site.getUrl(), emptyPage);
            readyLinks.add(site.getUrl());

            //Загрузка из базы обработанных ссылок
            for (int i = 1; i < links.size(); i++)
                //При записи в базу shortFormat: readyLinks.put(path.concat(links.get(i)), emptyPage);
                //readyLinks.put(links.get(i), emptyPage);
                readyLinks.add(links.get(i));
        });
        dialog.open();
    }

    public static String getLemmaString(String content, Lemmatization lemmatizator) {

        var list =
                lemmatizator.getHashMapsLemmaForEachCssSelector(content);

        var totalInfo = lemmatizator.mergeAllHashMaps(list);

        String totalString = "";
        for (Map.Entry<String, Lemmatization.LemmaInfo> entry : totalInfo.entrySet()) {
            Lemmatization.LemmaInfo lemmaInfo = entry.getValue();
            totalString += lemmaInfo.getLemma().concat(",")
                    .concat(lemmaInfo.getCount().toString()).concat(",")
                    .concat(lemmaInfo.getRank().toString()).concat(";");
        }
        return totalString;
    }

}
