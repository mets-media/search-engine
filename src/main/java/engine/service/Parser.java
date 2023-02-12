package engine.service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import engine.entity.*;
import engine.enums.LinkStatus;
import engine.enums.SiteStatus;
import engine.view.UIElement;
import lombok.RequiredArgsConstructor;
import org.jsoup.nodes.Document;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
    private static final ConcurrentHashMap<Integer, ConcurrentSkipListSet<String>> readyLinksHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Page>> pageHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ConcurrentSkipListSet<String>> inProcessLinksHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> errorLinksHashMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, DBWriter> dbWriterHashMap = new ConcurrentHashMap<>();
    private static Integer batchSize = 100;
    private static Integer delay;
    private static int parallelism = Runtime.getRuntime().availableProcessors();
    private static Boolean checkPartOfSpeech = true;

    private static final boolean saveLinksInShortFormat = false;
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

            System.out.printf("Старт для сайта: %s\n", site.getUrl());
            return;
        }

        //Возобновление сканирования
        ConcurrentSkipListSet<String> stopLinks = inProcessLinksHashMap.get(site.getId());

        stopLinks.addAll(beanAccess.getKeepLinkRepository().getPathsBySiteId(site.getId(), LinkStatus.ON_STOP_SCAN_LINK.ordinal()));

        if (!(stopLinks.size() == 0)) {
            stopLinks.forEach(p -> {
                //https://lenta.ru/
                System.out.println("Перезапуск: " + p);
                activePools.get(site.getId()).execute(new Parser(site, p, domainName));
            });
            beanAccess.getKeepLinkRepository().deleteBySiteId(site.getId());
            stopLinks.clear();

            DBWriter dbWriter = dbWriterHashMap.get(site.getId());
            dbWriter.start();
        } else {
            //stopLinks = 0 в базе есть одна страница - начальная!
            beanAccess.getSiteRepository().delete(site);
            site.setStatus(SiteStatus.INDEXING);
            beanAccess.getSiteRepository().save(site);
            Parser.start(site);
        }

        System.out.printf("Потоки запущены для сайта: %s\n", site.getUrl());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public static void writeToKeepLink(Integer siteId, LinkStatus status, List<String> links) {
        System.out.println("Запись onStopLink into KeepLink: " + links.size());
        String sql = "Insert into Keep_Link (Site_Id, Status, Path) values (?,?,?)";

        int[] result = beanAccess.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                String link = links.get(i);
                ps.setInt(1, siteId);
                ps.setInt(2, status.ordinal());
                ps.setString(3, link);
            }
            @Override
            public int getBatchSize() {
                return links.size();
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public static void writeToKeepLinkHM(Integer siteId,
                                         List<String> links,
                                         List<Integer> codes,
                                         LinkStatus status) {
        System.out.println("Запись errorlinks into KeepLink: " + links.size());
        String sql = "Insert into Keep_Link (Site_Id, Path, Code, Status) values (?,?,?,?)";

        int[] result = beanAccess.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setInt(1, siteId);
                ps.setString(2, links.get(i));
                ps.setInt(3, codes.get(i));
                ps.setInt(4, status.ordinal());
            }

            @Override
            public int getBatchSize() {
                return links.size();
            }
        });
    }

    public static void stop(Site site) {
        System.out.printf("!!! Стоп сайта: %s\n", site.getUrl());

        Integer siteId = site.getId();
        if (!activePools.containsKey(siteId))
            return;
        stopList.add(siteId);
        dbWriterHashMap.get(siteId).stopWriter();
    }

    public static Boolean indexingPage(Site site, String path) throws Exception {
        var code = HtmlParsing.getStatusCode(path, 10000);
        var document = HtmlParsing.getHtmlDocument(path);
        var content = document.toString();

        if (code == 200) {
            ConcurrentLinkedQueue<Page> readyPage = new ConcurrentLinkedQueue<>();
            readyPage.add(new Page(site.getId(), path, code, content));

            DBWriter dbWriter = new DBWriter("DBWriter[One page]", site, beanAccess,
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

    public static Boolean insertOrUpdatePage(String path) {

        Site findSite = null;

        for (Site site : beanAccess.getSiteRepository().findAll()) {
            String domainName = HtmlParsing.getDomainName(site.getUrl());
            if (domainName == null) return false;
            if (path.contains(domainName)) {
                findSite = site;
                break;
            }
        }

        if (!(findSite == null)) {
            Site finalFindSite = findSite;
            new Thread(() -> {
                try {
                    beanAccess.getPageRepository().deleteByPath(path);
                    if (indexingPage(finalFindSite, path)) {
                        beanAccess.getKeepLinkRepository().deleteByPath(path);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            return false;
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
        else {
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
        }

        inProcessLinks.remove(path);

        //Условие остановки!
        if (pool.isShutdown())  //Pool остановлен
            if (pool.getActiveThreadCount() == 1) { //это - последний активный поток!
                //Сообщение dbWriter о необходимости дописать оставшиеся readyPage
                dbWriter.writeAll();

                System.out.println("Запись inProcessLink");
                inProcessLinks.add(path); //Записать inProcessLinks включая текущую - она последняя
                writeToKeepLink(siteId, LinkStatus.ON_STOP_SCAN_LINK, inProcessLinks.stream().toList());

                System.out.println("Запись errorLinks");

                List<String> links = new ArrayList<>();
                List<Integer> codes = new ArrayList<>();

                errorLinks.forEach((key, value) -> {
                    links.add(key);
                    codes.add(value);
                });

                writeToKeepLinkHM(siteId, links, codes, LinkStatus.ERROR_LINK);

                System.out.println("readyPage.size: " + readyPages.size());
                System.out.println("inProcessLinks: " + inProcessLinks.size());
                System.out.println("Удаление activePools.remove(siteId)");
                activePools.remove(siteId);

                //Обновление статистики
                SearchService.refreshSitesInformation();
            }

        //Рекурсия
        Set<String> hReference = HtmlParsing.getAllLinks(document, domainName);
        if (hReference != null)
            for (String hRef : hReference) {
                if (!inProcessLinks.contains(hRef))  //очередь
                    if ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!readyLinks.contains(hRef))) {
                        inProcessLinks.add(hRef);
                        Parser parser = new Parser(site, hRef, domainName);
                        pool.execute(parser);
                    }
            }

        //Проверка на окончание загрузки
        if (inProcessLinks.size() == 0) {
            try {//Ожидание для возможности изменения readyPage - при перезапуске возможны ложные срабатывания
                sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if ((!(code == 200)) &&
                    (readyPages.size() == 0) &&
                    (errorLinks.size() == 1)) { //&&
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(SiteStatus.FAILED);
                site.setLastError("Не удалось загрузить стартовую страницу");
                beanAccess.getSiteRepository().save(site);

            } else {
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
                    System.out.println(site.getUrl() + " -> Загрузка завершена.");

                    SearchService.refreshSitesInformation();
                }
            }
        }
    }

    private static void showPrevDataDialog(Site site, Integer pageCount) {
        ConcurrentSkipListSet<String> readyLinks = readyLinksHashMap.get(site.getId());

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
            new Thread(() -> beanAccess.getPageRepository().deleteBySiteId(site.getId())).start();
        });
        cancel.addClickListener(clickEvent -> {
            dialog.close();
            UIElement.showMessage("Результаты сохранены");

            List<String> links = beanAccess.getPageRepository().getLinksBySiteId(site.getId());

            readyLinks.clear();
            readyLinks.addAll(links);
        });
        dialog.open();
    }
}
