package engine.service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import engine.entity.*;
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

    //private static Grid<Site> siteGrid;
    private static BeanAccess beanAccess;
    public final static String READY_LINKS_FILENAME = "links.txt";
    public final static String STOP_LINKS_FILENAME = "Stoplinks.txt";
    public final static String ERROR_LINKS_FILENAME = "Error_Links.txt";
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

    public static void setDataAccess(//Grid<Site> siteGrid,
                                     BeanAccess beanAccess) {
        //Parser.siteGrid = siteGrid;
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

        int pageCount = beanAccess.getPageRepository().countBySiteId(site.getId());

        //Все готовые страницы из базы данных
        readyLinks.addAll(beanAccess.getPageRepository().getLinksBySiteId(site.getId()));

        //if (pageCount > 0) // Если существуют страницы в базе данных
        //    showPrevDataDialog(site, pageCount);

        stopList.remove(site.getId());

        //beanAccess.getPageRepository().getLinksBySiteId(site)
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

        //stopLinks.addAll(beanAccess.getKeepLinkRepository().getPathsBySiteId(site.getId()));

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
        System.out.println("Записываю: " + links.size());
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

        //links.clear();
        //for (int i = 0; i < result.length; i++) System.out.println(result[i]);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public static void writeToKeepLinkHM(Integer siteId,
                                         List<String> links,
                                         List<Integer> codes,
                                         LinkStatus status) {
        System.out.println("Записываю: " + links.size());
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

        //links.clear();
        //for (int i = 0; i < result.length; i++) System.out.println(result[i]);
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
        dbWriterHashMap.get(siteId).stopWriter();
    }

    public static Boolean indexingPage(Site site, String path, BeanAccess beanAccess) throws Exception {
        var code = HtmlParsing.getStatusCode(path);
        var document = HtmlParsing.getHtmlDocument(path);
        var content = document.toString();

        if (code == 200) {
            ConcurrentLinkedQueue<Page> readyPage = new ConcurrentLinkedQueue<>();
            readyPage.add(new Page(site.getId(), path, code, content));

            DBWriter dbWriter = new DBWriter("DBWriter[One page]",site,beanAccess,
                    readyPage,1,true);
            dbWriter.start();

            //System.out.println(readyPage.size());

            while (readyPage.size() > 0) {
                sleep(2000);
            }
            dbWriter.stopWriter();
            return true;
        }
        return false;
    }

    public static Boolean insertOrUpdatePage(String path, BeanAccess beanAccess) {

        Site findSite = null;
        for (Site site : beanAccess.getSiteRepository().findAll()) {
            if (path.contains(HtmlParsing.getDomainName(site.getUrl()))) {
                findSite = site;
                break;
            }
        }

        if (!(findSite == null)) {
            Site finalFindSite = findSite;
            new Thread(() -> {
                try {
                    beanAccess.getPageRepository().deleteByPath(path);
                    indexingPage(finalFindSite, path, beanAccess);
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

        if (stopList.contains(siteId)) {
            pool.shutdown();
            return;
        }
        Document document = null;

        if (readyLinks.contains(path))
            //Переиндексация
            return;
        else {
            try {
                if (delay > 0)
                    sleep(delay);
                code = HtmlParsing.getStatusCode(path);
                document = HtmlParsing.getHtmlDocument(path);
                content = document.toString();
            } catch (Exception e) {
                //throw new RuntimeException(e);
                //e.printStackTrace();
                code = HtmlParsing.getStatusFromExceptionString(e.toString());
                errorLinks.put(path, code);
            }
            if (code == 200) {
                readyLinks.add(path);
                Page page = new Page(site.getId(), path, code, content);
                readyPages.add(page);
                //System.out.printf("%s -> readyLinks: %d, inProcessLinks: %d\n", domainName, readyLinks.size(), inProcessLinks.size());
                totalCountLinks.addAndGet(1L);
            }
        }

        inProcessLinks.remove(path);

        //Условие остановки!
        if (pool.isShutdown())  //Pool остановлен
            if (pool.getActiveThreadCount() == 1) { //я - последний поток!
                //Записать все readyPage
                System.out.println("Запись readyPage");

                //writeTempTable(siteId);
                //Отправить сообщение dbWriter об остановке и необходимости дописать оставшиеся page

                //Записать inProcessLinks включая текущую - она последняя
                System.out.println("Запись inProcessLink");
                inProcessLinks.add(path);
                writeToKeepLink(siteId, LinkStatus.ON_STOP_SCAN_LINK, inProcessLinks.stream().toList());

                System.out.println("Запись errorLinks");

                List<String> links = new ArrayList<>();
                List<Integer> codes = new ArrayList<>();

                errorLinks.entrySet().forEach(eLink -> {
                    links.add(eLink.getKey());
                    codes.add(eLink.getValue());
                });

                writeToKeepLinkHM(siteId, links, codes, LinkStatus.ERROR_LINK);

                System.out.println("readyPage.size: " + readyPages.size());
                System.out.println("inProccesLinks: " + inProcessLinks.size());
                System.out.println("Удаление activePools.remove(siteId)");
                activePools.remove(siteId);
            }

        //Рекурсия

        Set<String> hReference = HtmlParsing.getAllLinks(document, domainName);
        if (hReference != null)
            for (String hRef : hReference) {
                if (!inProcessLinks.contains(hRef))  //очередь ожидания
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
                //(errorLinks.first().substring(4).equals(path))) {
                site.setStatusTime(LocalDateTime.now());
                site.setStatus(SiteStatus.FAILED);
                site.setLastError("Не удалось загрузить стартовую страницу");
                beanAccess.getSiteRepository().save(site);
                System.out.println("Не удалось загрузить стартовую страницу");

            } else {
                try {
                    sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if  (inProcessLinks.size() == 0) { //Проверочный вариант
                    site.setStatus(SiteStatus.INDEXED);
                    beanAccess.getSiteRepository().save(site);
                    stop(site);
                    System.out.println(site.getUrl() + " -> Загрузка завершена.");
                }
            }
            //siteGrid.setItems(beanAccess.getSiteRepository().findAll());
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
