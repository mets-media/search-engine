package engine.service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import engine.entity.Page;
import engine.entity.Site;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import engine.views.MainView;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sound.midi.Soundbank;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;

//@Component
@Service
@RequiredArgsConstructor
public class Parser extends RecursiveAction {
    private static JdbcTemplate jdbcTemplate;
    public final static String READY_LINKS_FILENAME = "links.txt";
    public final static String STOP_LINKS_FILENAME = "Stoplinks.txt";
    public final static String ERROR_LINKS_FILENAME = "Error_Links.txt";
    private static HashMap<Integer, ForkJoinPool> activePools = new HashMap<>();
    private static ConcurrentHashMap<Integer, ConcurrentHashMap<String, Page>> readyLinksHashMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, ConcurrentSkipListSet> inProcessLinksHashMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, ConcurrentSkipListSet> errorLinksHashMap = new ConcurrentHashMap<>();
    private Site site;
    private String path;
    private Integer code;
    private String content;
    private String domainName;
    public static Long maxMemory;
    public static Runtime runtime;
    public static Page emptyPage = new Page(-1,"",-1,"");
    private static ConcurrentHashMap<String, Page> cache = new ConcurrentHashMap<>();


    private static HashSet<Integer> stopList = new HashSet<>();
    private static AtomicLong totalCountLinks = new AtomicLong();
    private static PageRepository pageRepository;
    private static SiteRepository siteRepository;

    public static Set<Integer> getStopList() {
        return stopList;
    }

    public static void setPageRepository(PageRepository pageRepository) {
        Parser.pageRepository = pageRepository;
    }

    public static void setSiteRepository(SiteRepository siteRepository) {
        Parser.siteRepository = siteRepository;
    }

    public static void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        Parser.jdbcTemplate = jdbcTemplate;
    }

    public Parser(Site site, String path, String domainName) {
        this.jdbcTemplate = jdbcTemplate;
        this.path = path;
        this.domainName = domainName;
        this.site = site;
        runtime = Runtime.getRuntime();
        maxMemory = runtime.maxMemory();
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
                                       List<String> links,
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

    public static void saveStopLinksToFile(String fileName,
                                           String domainName,
                                           ConcurrentSkipListSet links,
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


//        try {
//            OutputStream f = new FileOutputStream(fileName, true);
//            OutputStreamWriter writer = new OutputStreamWriter(f);
//            BufferedWriter out = new BufferedWriter(writer);
//
//            //out.append(skipListSet.toString());
//            //out.flush();
//
//            readyLinks.forEach(l -> {
//                String link = l.toString();
//
//                String shortLink = link.substring(link.indexOf(domainName) + domainName.length());
//                if ("".equals(shortLink))
//                    if (link.contains("//".concat(domainName)))
//                        shortLink = "/";
//                    else
//                        shortLink = link;
//
//                if ("/".equals(shortLink)) {
//                    if (link.contains(".".concat(domainName)))
//                        shortLink = link;
//                }
//
//                try {
//                    out.write(shortLink.concat(" ->").concat(link).concat("\n"));
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
//
//
//            out.flush();
//
//        } catch (IOException e) {
//            System.err.println(e);
//        }
    }

    public static void initSite(Site site, String readyLinksFilename, String stopLinksFilename) {

        int siteId = site.getId();
        if (activePools.containsKey(siteId)) {
            readyLinksHashMap.get(siteId).clear();
            inProcessLinksHashMap.get(siteId).clear();
            errorLinksHashMap.get(siteId).clear();
        }
        activePools.put(siteId, new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null, true));
        inProcessLinksHashMap.put(siteId, new ConcurrentSkipListSet());
        readyLinksHashMap.put(siteId, new ConcurrentHashMap<>());
        errorLinksHashMap.put(siteId, new ConcurrentSkipListSet());

        ConcurrentHashMap<String, Page> rLinksHashMap = new ConcurrentHashMap<>();
        if (Files.exists(Paths.get(readyLinksFilename))) {
            loadLinksFromFile(readyLinksFilename).forEach(l -> rLinksHashMap.put(l, new Page()));
            readyLinksHashMap.get(site.getId()).putAll(rLinksHashMap);
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

        ConcurrentHashMap<String, Page> readyLinks = readyLinksHashMap.get(site.getId());

        int pageCount = pageRepository.countBySiteId(site.getId());

        if (pageCount > 0) { // Если существуют страницы в базе данных
            Dialog dialog = new Dialog();
            Button confirm = new Button("Удалить");
            Button cancel = new Button("Продолжить");

            dialog.add("Удалить результаты предыдущего сканирования?");
            dialog.add(confirm);
            dialog.add(cancel);
            confirm.addClickListener(clickEvent -> {
                dialog.close();
                Notification notification = new Notification("Удалено", 500);
                notification.setPosition(Notification.Position.MIDDLE);
                notification.open();
                pageRepository.deleteBySiteId(site.getId());
            });
            cancel.addClickListener(clickEvent -> {
                dialog.close();
                Notification notification = new Notification("Результаты сохранены", 500);
                notification.setPosition(Notification.Position.MIDDLE);
                notification.open();

                List<String> links = pageRepository.findLinksBySiteId(site.getId());

                //readyLinks.add("/");
                readyLinks.put("/", new Page());
                for (int i = 1; i < links.size(); i++) {
                    //readyLinks.add(path.concat(links.get(i)));
                    readyLinks.put(path.concat(links.get(i)), new Page());
                }
            });
            dialog.open();
        }

        try {
            Files.createDirectories(Paths.get("data/" + domainName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        TimeMeasure.setStartTime();
        stopList.remove(site.getId());
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


//        System.out.println("Завершение потоков!");
//        System.out.format("Страницы сайта загружены за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        //Запись Cache.obj
        //TimeMeasure.setStartTime();
        //saveCache(fileName);
        //System.out.format("Cache.obj записан за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        //Зпись в базу
        //savePagesFromCash();


    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public static void writeToDatabase(List<Page> pages) {
        System.out.println("Записываю: " + pages.size());
        String sql = "Insert into Page (Site_Id, Code, Path, Content) values (?,?,?,?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Page page = pages.get(i);
                Integer siteId = page.getSiteId();
                ps.setInt(1, siteId);
                Integer code = page.getCode();
                ps.setInt(2, code);
                ps.setString(3, page.getPath());
                ps.setString(4, page.getContent());
            }

            @Override
            public int getBatchSize() {
                return pages.size();
            }
        });
    }

    public static void stop(Site site) {
        System.out.printf("!!!!!!!!!!!! Стоп сайта: %s\n", site.getUrl());
        String domainName = HtmlParsing.getDomainName(site.getUrl());

        if (!activePools.containsKey(site.getId())) return;
        deleteFile("data/" + domainName + "/" + STOP_LINKS_FILENAME);

        stopList.add(site.getId());
        ConcurrentHashMap<String, Page> readyLinks = readyLinksHashMap.get(site.getId());
        if (!(readyLinks == null))
            saveLinksToFile("data/" + domainName + "/" + READY_LINKS_FILENAME,
                    domainName,
                    new ArrayList<>(readyLinks.keySet()),
                    false,
                    SaveFileMode.REWRITE);

        TimeMeasure.setStartTime();
        writeToDatabase(getLinksSetEmptyPage(readyLinks));
        System.out.format("Запись в базу за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

    }

    public void stopScanSite(Site site) {
        if (!activePools.containsKey(site.getId())) {
            return;
        }

        //удаляем ссылки предыдушего стопа
        deleteFile("data/" + domainName + "/" + STOP_LINKS_FILENAME);
        //останавливаем сканирование
        stopList.add(site.getId());
        System.out.printf("Стоп сканирования для сайта: %s\n", site.getUrl());

        TimeMeasure.setStartTime();
        String domainName = HtmlParsing.getDomainName(site.getUrl());

        ConcurrentHashMap<String, Page> readyLinks = readyLinksHashMap.get(site.getId());
        if (!(readyLinks == null))
            saveLinksToFile("data/" + domainName + "/" + READY_LINKS_FILENAME,
                    domainName,
                    new ArrayList<>(readyLinks.keySet()),
                    false,
                    SaveFileMode.REWRITE);


//        saveHashMapToFile("data/" + domainName + "/" + READY_LINKS_FILENAME,
//                domainName, readyLinksHashMap.get(site.getId()), false, SaveFileMode.REWRITE);
        System.out.format(READY_LINKS_FILENAME + " записан за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        TimeMeasure.setStartTime();
        //writeToDatabase(readyLinks.values().stream().toList());
        writeToDatabase(getLinksSetEmptyPage(readyLinks));
        System.out.format("запись в базу данных за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));
    }

    private static List<Page> getLinksSetEmptyPage(ConcurrentHashMap<String,Page> pageHashMap) {
        List<Page> listPage = new ArrayList<>();
        Iterator<Page> iterator = pageHashMap.values().iterator();
        while (iterator.hasNext()) {
            Page page = iterator.next();
            if (!page.equals(emptyPage)) {
                listPage.add(page);
                pageHashMap.replace(page.getPath(),emptyPage);
            }
        }
        System.out.println("Передаю на запись " + listPage.size());
        return listPage;
    }
    @Override
    protected void compute() {
        int siteId = site.getId();

        ForkJoinPool pool = activePools.get(siteId);

        ConcurrentSkipListSet<String> inProcessLinks = inProcessLinksHashMap.get(siteId);

        ConcurrentHashMap<String, Page> readyLinks = readyLinksHashMap.get(siteId);

        if (stopList.contains(siteId)) {
            pool.shutdown();
            saveStopLinksToFile("data/" + domainName + "/" + STOP_LINKS_FILENAME,
                    domainName,
                    inProcessLinks,
                    false,
                    SaveFileMode.DO_NOT_REWRITE);
            return;
        }

        Document document = null;
        inProcessLinks.add(path);

        if (!readyLinks.keySet().contains(path)) {
            try {
                code = HtmlParsing.getStatusCode(path);
//                System.out.printf("число пулов %d, Активных потоков: %d         ",
//                        activePools.size(), pool.getActiveThreadCount());
//                System.out.format(totalCountLinks + ": download: [%d]  %s\n", code, path);
                document = HtmlParsing.getHtmlDocument(path);
                content = document.body().toString();

            } catch (Exception e) {
                //throw new RuntimeException(e);
//                e.printStackTrace();
                code = HtmlParsing.getStatusFromExceptionString(e.toString());
                String errorLinkString;
                if (code == -2) {
                    errorLinkString = "Read timed out".concat(": ").concat(path);
                } else
                    errorLinkString = code.toString().concat(": ").concat(path);
                errorLinksHashMap.get(siteId).add(errorLinkString);

                saveLinksToFile("data/" + domainName + "/" + ERROR_LINKS_FILENAME,
                        domainName,
                        new ArrayList<>(errorLinksHashMap.get(siteId)),
                        false,
                        SaveFileMode.APPEND);

                System.out.println("Exception! :" + e);
                System.out.println(path);
                if (inProcessLinks.remove(path))
                    System.out.println("Exception: inProcess.remove: " + path);
                else
                    System.out.println("Exception: Не удалено: " + path);
            }
            if (code == 200) {
                readyLinks.put(path, new Page(site.getId(), path, code, content));
                //readyLinks.put(path, new Page(site.getId(), path, code, ""));
                inProcessLinks.remove(path);

                System.out.printf("%s -> readyLinks: %d, inProcessLinks: %d\n", domainName, readyLinks.size(), inProcessLinks.size());

                //Сайт полностью отсканирован - проверить 0 или 1
//                if ((inProcessLinks.size() == 0) && (readyLinks.size() > 0)) {
//                    System.out.println("Завершение сканирования " + domainName);
//                    stopScanSite(site);
//                }

                totalCountLinks.addAndGet(1L);
                if (readyLinks.size() % 500 == 0) {

                    System.out.println("Запись в базу данных");
                    TimeMeasure.setStartTime();
                    writeToDatabase(getLinksSetEmptyPage(readyLinks));
                    System.out.println(domainName + " -> время последней записи: " + TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

                    site.setPageCount(readyLinks.size());
                    siteRepository.save(site);

                    //MainView.getGrid().setItems(siteRepository.findAll());
                }
            }
        } else {
            //Если ссылка есть в readyLinks - то не следует ничего делать!
            return;
        }
        Set<String> hReference = HtmlParsing.getAllLinks(document, domainName);
        if (hReference != null)
            for (String hRef : hReference) {
                if (!inProcessLinks.contains(hRef))
                    if ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!readyLinks.keySet().contains(hRef))) {
                        Parser parser = new Parser(site, hRef, domainName);
                        pool.execute(parser);
                    }
            }
    }

    public static void loadCache(String fileName) {
        cache.clear();
        try {
            if (Files.exists(Paths.get(fileName))) {
                TimeMeasure.setStartTime();
                FileInputStream fileInputStream = new FileInputStream(fileName);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                cache = (ConcurrentHashMap<String, Page>) objectInputStream.readObject();

                List<String> listHTML = new ArrayList<>();
                StringBuilder stringBuilder = new StringBuilder();
                cache.entrySet().forEach(l -> {
                    //stringBuilder.append(l.getKey());
                    //stringBuilder.append("\n\n");
                    listHTML.add(l.getKey());
                });

                Collections.sort(listHTML);

                listHTML.forEach(l -> stringBuilder.append(l.concat("\n")));

                try {
                    Path path = Paths.get(fileName.replace(FilenameUtils.getExtension(fileName), "txt"));
                    Files.writeString(path, stringBuilder, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.format("Страниц в файле %d\n", cache.size());
                System.out.format("Cache.obj загружен за %d сек.\n", TimeMeasure.getExperienceTime() / 1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void saveCache(String fileName) {

        String saveDirectory = fileName.replace("/cache.obj", "");

        try {
            Files.createDirectories(Paths.get(saveDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try { //Сохранение заполненного Heap
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(cache);

            fileOutputStream.close();
            objectOutputStream.close();

            System.out.format("В cache.obj записано %d страниц.\n", cache.size());

//            FileInputStream fileInputStream = new FileInputStream(fileName);
//            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
//            ConcurrentHashMap<String, Link> readingHeap = (ConcurrentHashMap<String, Link>) objectInputStream.readObject();
//
//            fileInputStream.close();
//            objectInputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
