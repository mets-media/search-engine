package engine.service;

import engine.entity.Config;
import engine.entity.KeepLink;
import engine.entity.Page;
import engine.entity.Site;
import engine.enums.LinkStatus;
import engine.enums.SiteStatus;
import engine.repository.DBWriter;
import engine.view.UIElement;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Thread.sleep;

@Service
@RequiredArgsConstructor
public class Parser extends RecursiveAction {
    private Site site;
    private String path;
    private String content;
    private Integer code;
    private String domainName;
    private static int timeout;
    private static BeanAccess beanAccess;
    private static final HashMap<Integer, ForkJoinPool> activePools = new HashMap<>();
    private static final ConcurrentHashMap<Integer,
            ConcurrentSkipListSet<String>> readyLinksHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer,
            ConcurrentLinkedQueue<Page>> pageHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer,
            ConcurrentSkipListSet<String>> inProcessLinksHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer,
            ConcurrentHashMap<String, Integer>> errorLinksHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, DBWriter> dbWriterHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Page> reindexPages = new ConcurrentLinkedQueue<>();
    private static Boolean reindexStarting = false;
    private static Integer batchSize = 100;
    private static Integer delay;
    private static int parallelism = Runtime.getRuntime().availableProcessors();
    private static Boolean checkPartOfSpeech = true;
    private static final HashSet<Integer> stopList = new HashSet<>();
    private static final AtomicLong totalCountLinks = new AtomicLong();

    public static Set<Integer> getStopList() {
        return stopList;
    }

    public static void setBeanAccess(BeanAccess beanAccess) {
        Parser.beanAccess = beanAccess;
    }

    public Parser(Site site, String path, String domainName) {
        this.path = path;
        this.domainName = domainName;
        this.site = site;
    }

    private static void readConfigVariables() {
        List<Config> configList = beanAccess.getConfigRepository().findAll();
        configList.forEach(configLine -> {
            switch (configLine.getKey()) {
                case "T.out" -> {
                    try {
                        timeout = Integer.parseInt(configLine.getValue());
                        HtmlParsing.setTimeout(timeout);
                    } catch (Exception e) {
                        UIElement.showMessage("Тип свойства 'Timeout' должен быть Integer");
                    }
                }
                case "delay" -> {
                    delay = Integer.parseInt(configLine.getValue());
                }
                case "tps" -> {
                    try {
                        parallelism = Integer.parseInt(configLine.getValue());
                    } catch (Exception e) {
                        UIElement.showMessage("Тип свойства 'tps' должен быть Integer");
                    }
                }
                case "batch" -> {
                    try {
                        batchSize = Integer.parseInt(configLine.getValue());
                    } catch (Exception e) {
                        UIElement.showMessage("Тип свойства 'batch' должен быть Integer");
                    }
                }
                case "isPoS" -> {
                    try {
                        checkPartOfSpeech = Boolean.parseBoolean(configLine.getValue());
                    } catch (Exception e) {
                        UIElement.showMessage("Тип свойства 'isPoS' должен быть true/false!");
                    }
                }
            }
        });
    }

    public static void initSite(Site site) {

        int siteId = site.getId();

        readConfigVariables();

        activePools.put(siteId, new ForkJoinPool(parallelism,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null, true));

        inProcessLinksHashMap.put(siteId, new ConcurrentSkipListSet<>());

        readyLinksHashMap.put(siteId, new ConcurrentSkipListSet<>());

        pageHashMap.put(siteId, new ConcurrentLinkedQueue<>());

        errorLinksHashMap.put(siteId, new ConcurrentHashMap<>());

        dbWriterHashMap.put(siteId, new DBWriter("DBWriter[" + HtmlParsing.getDomainName(site.getUrl()) + "]",
                site,
                beanAccess,
                pageHashMap.get(siteId),
                batchSize,
                checkPartOfSpeech));
    }


    public static void start(Site site) {
        String path = site.getUrl();
        String domainName = HtmlParsing.getDomainName(path);
        //Инициализация статических переменных для класса Parser
        initSite(site);
        var readyLinks = readyLinksHashMap.get(site.getId());
        //Все готовые страницы из базы данных
        readyLinks.addAll(beanAccess.getPageRepository().getLinksBySiteId(site.getId()));

        stopList.remove(site.getId());

        Parser parser;
        if (readyLinks.size() == 0) { //Запуск с нуля
            parser = new Parser(site, path, domainName);
            activePools.get(site.getId()).execute(parser);
            dbWriterHashMap.get(site.getId()).start();
            return;
        }
        //Возобновление сканирования
        ConcurrentSkipListSet<String> stopLinks = inProcessLinksHashMap.get(site.getId());
        stopLinks.addAll(beanAccess.getKeepLinkRepository().getPathsBySiteId(site.getId(),
                LinkStatus.ON_STOP_SCAN_LINK.ordinal()));

        if (!(stopLinks.size() == 0)) {
            stopLinks.forEach(p -> {
                activePools.get(site.getId()).execute(new Parser(site, p, domainName));
            });
            beanAccess.getKeepLinkRepository().deleteBySiteId(site.getId());
            stopLinks.clear();

            DBWriter dbWriter = dbWriterHashMap.get(site.getId());
            dbWriter.start();
        } else {
            beanAccess.getSiteRepository().delete(site);
            site.setStatus(SiteStatus.INDEXING);
            beanAccess.getSiteRepository().save(site);
            Parser.start(site);
        }
    }


    public static void stop(Site site) {
        Integer siteId = site.getId();
        if (!activePools.containsKey(siteId))
            return;
        stopList.add(siteId);
        dbWriterHashMap.get(siteId).stopWriter();
    }

    public static Boolean indexingPage(Site site, String path) throws Exception {
        var code = HtmlParsing.getStatusCode(path, timeout * 3);
        var document = HtmlParsing.getHtmlDocument(path);
        var content = document.toString();

        if (code == 200) {
            ConcurrentLinkedQueue<Page> readyPage = new ConcurrentLinkedQueue<>();
            readyPage.add(new Page(site.getId(), path, code, content));

            DBWriter dbWriter = new DBWriter("DBWriter[indexing]", site, beanAccess,
                    readyPage, 1, true);
            dbWriter.start();

            while (readyPage.size() > 0) {
                sleep(2000);
            }
            dbWriter.stopWriter();
            return true;
        }
        return false;
    }

    public static Boolean idxPage(Site site, String path) throws Exception {
        var code = HtmlParsing.getStatusCode(path, timeout * 3);
        var document = HtmlParsing.getHtmlDocument(path, timeout * 3);
        var content = document.toString();
        if (code == 200) {
            reindexPages.add(new Page(site.getId(), path, code, content));
            return true;
        }
        return false;
    }


    private static Site findSite(String path) {
        Site findSite = null;
        for (Site site : beanAccess.getSiteRepository().findAll()) {

            String domainName = HtmlParsing.getDomainName(site.getUrl());
            if (domainName == null) return null;

            if (path.contains(domainName)) {
                findSite = site;
                break;
            }
        }
        return findSite;
    }

    public static Boolean insertOrUpdatePage(String path) {

        Site finalFindSite = findSite(path); //проверка принадлежности к сайтам в базе данных

        if (finalFindSite == null) return false;

        new Thread(() -> {
            try {
                beanAccess.getPageRepository().deleteByPath(path);
                if (idxPage(finalFindSite, path)) {
                    beanAccess.getKeepLinkRepository().deleteByPath(path);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();

        if (!reindexStarting) {
            reindexStarting = true;
            DBWriter dbWriter = new DBWriter("DBWriter[reindex]", null, beanAccess,
                    reindexPages, 1, true);
            dbWriter.start();
        }

        return true;
    }

    @Override
    protected void compute() {
        int siteId = site.getId();
        var errorLinks = errorLinksHashMap.get(siteId);

        ForkJoinPool pool = activePools.get(siteId);
        var inProcessLinks = inProcessLinksHashMap.get(siteId);
        var readyLinks = readyLinksHashMap.get(siteId);
        var readyPages = pageHashMap.get(siteId);
        var dbWriter = dbWriterHashMap.get(siteId);

        if (stopList.contains(siteId)) {
            pool.shutdown();
            return;
        }
        Document document = null;

        if (readyLinks.contains(path))
            return;

        try {
            if (delay > 0)
                sleep(delay);
            code = HtmlParsing.getStatusCode(path);
            document = HtmlParsing.getHtmlDocument(path);
            content = document.toString();

        } catch (Exception e) {
            code = HtmlParsing.getStatusFromExceptionString(e.toString());
            errorLinks.put(path, code);
            new Thread(() -> {/** записываем ссылку с кодом ошибки **/
                beanAccess.getKeepLinkRepository()
                        .save(new KeepLink(siteId, code, LinkStatus.ERROR_LINK.ordinal(), path));
            }).start();
        }

        if (code == 200) {
            readyLinks.add(path);
            Page page = new Page(site.getId(), path, code, content);
            readyPages.add(page);
            totalCountLinks.addAndGet(1L);
        }

        inProcessLinks.remove(path);

        //Условие остановки!
        if (pool.isShutdown())
            if (pool.getActiveThreadCount() == 1) { //это - последний активный поток!
                //Сообщение dbWriter о необходимости дописать оставшиеся readyPage
                dbWriter.writeAll();

                inProcessLinks.add(path); //Записать inProcessLinks включая текущую - она последняя
                beanAccess.getImplRepository()
                        .writeToKeepLink(siteId, LinkStatus.ON_STOP_SCAN_LINK, inProcessLinks.stream().toList());

                List<String> links = new ArrayList<>();
                List<Integer> codes = new ArrayList<>();

                errorLinks.forEach((key, value) -> {
                    links.add(key);
                    codes.add(value);
                });

                beanAccess.getImplRepository()
                        .writeToKeepLinkHM(siteId, links, codes, LinkStatus.ERROR_LINK);
                activePools.remove(siteId);
                //Обновление статистики
                SearchService.refreshSitesInformation();
            }

        //Рекурсия
        Set<String> hReference = HtmlParsing.getAllLinks(document);
//        if (hReference != null)
//            for (String hRef : hReference) {
//                if (!inProcessLinks.contains(hRef))  //очередь
//                    if ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!readyLinks.contains(hRef))) {
//                        inProcessLinks.add(hRef);
//                        Parser parser = new Parser(site, hRef, domainName);
//                        pool.execute(parser);
//                    }
//            }

        if (hReference != null)
            for (String hRef : hReference) {
                if (!inProcessLinks.contains(hRef) &&
                        ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!readyLinks.contains(hRef)))) {
                    inProcessLinks.add(hRef);
                    Parser parser = new Parser(site, hRef, domainName);
                    pool.execute(parser);
                }
            }

        if (inProcessLinks.size() != 0) return;

        //Проверка на окончание загрузки
        try {//Ожидание для возможности изменения readyPage - при перезапуске возможны ложные срабатывания
            sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if ((!(code == 200)) &&
                (readyPages.size() == 0) &&
                (errorLinks.size() == 1)) {
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(SiteStatus.FAILED);
            site.setLastError("Не удалось загрузить стартовую страницу");
            beanAccess.getSiteRepository().save(site);
            return;
        }

        try {
            sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if ((pool.getActiveThreadCount() == 1) &&
                (inProcessLinks.size() == 0)) {
            site.setStatus(SiteStatus.INDEXED);
            beanAccess.getSiteRepository().save(site);
            stop(site);
            SearchService.refreshSitesInformation();
        }
    }
}
