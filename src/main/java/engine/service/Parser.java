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
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class Parser extends RecursiveAction {
    public final static String READY_LINKS_FILENAME = "links.txt";
    public final static String STOP_LINKS_FILENAME = "Stoplinks.txt";
    private static HashMap<Integer, ForkJoinPool> activePools = new HashMap<>();
    private static ConcurrentHashMap<Integer, ConcurrentSkipListSet> readyLinksHashMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, ConcurrentSkipListSet> inProcessLinksHashMap = new ConcurrentHashMap<>();

    //private static ArrayList<Page>

    //private static ConcurrentSkipListSet inProcessLinks = new ConcurrentSkipListSet();
    private Site site;
    private String path;
    private Integer code;
    private String content;
    private String domainName;
    private static ConcurrentHashMap<String, Page> cache = new ConcurrentHashMap<>();
    //private ConcurrentSkipListSet readyLinks = new ConcurrentSkipListSet();

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

    public Parser(Site site, String path, String domainName) {
        this.path = path;
        this.domainName = domainName;
        this.site = site;
        if (readyLinksHashMap.get(site.getId()) == null)
            readyLinksHashMap.put(site.getId(), new ConcurrentSkipListSet());
    }

    public Parser(Site site, String path, String domainName, List<String> readyLinks) {
        this(site, path, domainName);
        readyLinksHashMap.get(site.getId()).addAll(readyLinks);
    }

    public void stopScanSite(Site site) {
        stopList.add(site.getId());
        System.out.printf("Стоп сканирования для сайта: %s\n", site.getUrl());

        TimeMeasure.setStartTime();
        String domainName = HtmlParsing.getDomainName(site.getUrl());
        deleteFile("data/" + domainName + "/" + STOP_LINKS_FILENAME);

        ConcurrentSkipListSet l = readyLinksHashMap.get(site.getId());
        saveLinksToFile("data/" + domainName + "/" + READY_LINKS_FILENAME,
                domainName, l, false, SaveFileMode.REWRITE);
        System.out.format(READY_LINKS_FILENAME + " записан за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));
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

    public static void saveLinksToFile(String fileName,
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

    public static void start(Site site) {

        String path = site.getUrl();
        String domainName = HtmlParsing.getDomainName(path);

        String readyLinksFilename = "data/" + domainName + "/" + READY_LINKS_FILENAME;

        List<String> readyLinks;
        //=========================================================
        // Наличие файла - продолжение сканирования
        if (Files.exists(Paths.get(readyLinksFilename)))
            readyLinks = loadLinksFromFile(readyLinksFilename);
        else//Отсутсивие файла - новое сканирование
            readyLinks = new ArrayList<>();

        if (!activePools.containsKey(site)) {
            int siteId = site.getId();
            activePools.put(siteId, new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                    null, true));
            inProcessLinksHashMap.put(siteId, new ConcurrentSkipListSet());
        }

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

                readyLinks.add("/");
                for (int i = 1; i < links.size(); i++) {
                    readyLinks.add(path.concat(links.get(i)));
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
        } else {
            //TODO Подгрузка readyLinks
            //Вызов конструктора в котором создаются и заполняются необходимые статические элементы
            new Parser(site, path, domainName, readyLinks);

            String stopLinksFilename = "data/" + domainName + "/" + STOP_LINKS_FILENAME;
            if (Files.exists(Paths.get(stopLinksFilename))) {
                List<String> sList = loadLinksFromFile(stopLinksFilename);
                sList.forEach(p -> {
                    activePools.get(site.getId()).execute(new Parser(site, p, domainName));
                });

                try {//необходимо удалить файл поскольку применяетя DO_NOT_REWRITE
                    Files.delete(Paths.get(stopLinksFilename));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            //activePools.get(site.getId()).execute(parser);
        }
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

    @Override
    protected void compute() {
        int siteId = site.getId();
        ForkJoinPool pool = activePools.get(siteId);
        ConcurrentSkipListSet<String> inProcessLinks = inProcessLinksHashMap.get(siteId);
        ConcurrentSkipListSet<String> readyLinks = readyLinksHashMap.get(siteId);

        if (stopList.contains(siteId)) {
            pool.shutdown();
            saveLinksToFile("data/" + domainName + "/" + STOP_LINKS_FILENAME, domainName, inProcessLinks, false, SaveFileMode.DO_NOT_REWRITE);
            //System.out.printf("Site %s: inProcessLinks: %d -> %s\n", site.getUrl(), inProcessLinks.size(), path);
//            System.out.printf("======= %s ======\n", site.getUrl());
//            inProcessLinks.forEach(p-> System.out.println(p));
//            System.out.println("====================");
            return;
        }

        Document document = null;
        inProcessLinks.add(path);

        if (!readyLinks.contains(path)) {
            try {
                code = HtmlParsing.getStatusCode(path);
                System.out.printf("число пулов %d, Активных потоков: %d         ",
                        activePools.size(), pool.getActiveThreadCount());
                System.out.format(totalCountLinks + ": download: [%d]  %s\n", code, path);
                document = HtmlParsing.getHtmlDocument(path);
                content = document.body().toString();

            } catch (Exception e) {
                //throw new RuntimeException(e);
//                e.printStackTrace();
                code = HtmlParsing.getStatusFromExceptionString(e.toString());

                System.out.println("Exception! :" + e);
                System.out.println(path);
            }
            if (code == 200) {

                readyLinks.add(path);
                inProcessLinks.remove(path);

                totalCountLinks.addAndGet(1L);
                if (readyLinks.size() % 10 == 0) {
                    site.setPageCount(readyLinks.size());
                    siteRepository.save(site);
                    MainView.gridRefresh();
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
                    if ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!readyLinks.contains(hRef))) {
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
