package engine.service;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import engine.entity.Page;
import engine.entity.Site;
import engine.repository.PageRepository;

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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
//public class Parser extends RecursiveTask<String> {
public class Parser extends RecursiveAction {
    private Integer siteId;
    private String path;
    private Integer code;
    private String content;
    private String domainName;
    private static ConcurrentHashMap<String, Page> cache = new ConcurrentHashMap<>();
    private static ConcurrentSkipListSet skipListSet = new ConcurrentSkipListSet();

    private static ConcurrentSkipListSet inProgressListSet = new ConcurrentSkipListSet();

    private Runtime runtime = Runtime.getRuntime();


    private static AtomicLong countLinks = new AtomicLong();

    private static PageRepository pageRepository;
    private static volatile boolean active;

    public static ConcurrentHashMap<String, Page> getCache() {
        return cache;
    }

    public static ConcurrentSkipListSet getSkipListMap() {
        return skipListSet;
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean active) {
        Parser.active = active;
    }

    public static void setPageRepository(PageRepository pageRepository) {
        Parser.pageRepository = pageRepository;
    }

    public Parser(Integer siteId, String path, String domainName) {
        this.path = path;
        this.domainName = domainName;
        this.siteId = siteId;

    }

    public static void saveLinksToFile(String fileName, String domainName) {
        try {
            OutputStream f = new FileOutputStream(fileName, true);
            OutputStreamWriter writer = new OutputStreamWriter(f);
            BufferedWriter out = new BufferedWriter(writer);

            //out.append(skipListSet.toString());
            //out.flush();


            skipListSet.forEach(l -> {
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

                try {
                    out.write(shortLink.concat(" ->").concat(link).concat("\n"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            out.flush();


        } catch (IOException ex) {
            System.err.println(ex);
        }

    }

    public static void start(int siteId, String path) {
        skipListSet.clear();
        inProgressListSet.clear();

        int pageCount = pageRepository.countBySiteId(siteId);

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
                pageRepository.deleteBySiteId(siteId);
            });
            cancel.addClickListener(clickEvent -> {
                dialog.close();
                Notification notification = new Notification("Результаты сохранены", 500);
                notification.setPosition(Notification.Position.MIDDLE);
                notification.open();

                List<String> links = pageRepository.findLinksBySiteId(siteId);
                skipListSet.add("/");
                for (int i = 1; i < links.size(); i++) {
                    skipListSet.add(path.concat(links.get(i)));
                }
            });
            dialog.open();

        }


        String domainName = HtmlParsing.getDomainName(path);
        String fileName = "data/" + domainName + "/hRef.txt";

        try {
            Files.createDirectories(Paths.get("data/" + domainName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        active = true;

        TimeMeasure.setStartTime();
        Parser parser = new Parser(siteId, path, domainName);
        parser.invoke();

        System.out.println("Завершение потоков!");

        System.out.format("Страницы сайта загружены за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        //Запись Cache.obj
        //TimeMeasure.setStartTime();
        //saveCache(fileName);
        //System.out.format("Cache.obj записан за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        //Зпись в базу
        //savePagesFromCash();

        TimeMeasure.setStartTime();
        saveLinksToFile("data/" + domainName + "/links.txt", domainName);
        System.out.format("Links.txt записан за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

    }

    @Override
    //protected String compute() {
    protected void compute() {
        Document document = null;
        Page page = null;
        inProgressListSet.add(path);

        if (!skipListSet.contains(path)) {
            try {
                code = HtmlParsing.getStatusCode(path);

                System.out.format(countLinks + ": download: [%d]  %s\n", code, path);

                document = HtmlParsing.getHtmlDocument(path);
                content = document.body().toString();

            } catch (Exception e) {
                //throw new RuntimeException(e);
                //e.printStackTrace();
                content = "";
                code = HtmlParsing.getStatusFromExceptionString(e.toString());
                System.out.println("Exception! :" + e);
                System.out.println(path);
            }
            if (code != null) { //Таймаут сервера и т.п.

//                String shortPath = path.substring(path.indexOf(domainName) + domainName.length());
//                if ("".equals(shortPath))
//                    if (path.contains(domainName))
//                        shortPath = "/";
//                page = new Page(parentId, shortPath, code, content);

                if (code == 200) {
                    skipListSet.add(path);

                    countLinks.addAndGet(1L);
                    if (countLinks.compareAndSet(10, 10)) {
                        saveLinksToFile("data/" + domainName + "/links.txt", domainName);
                    }


                    if (countLinks.get() % 10 == 0) {
                        // TODO: 28.10.2022 Записать значение в БД

                        MainView.gridRefresh();
                    }

                    //временно убрал запись в базу
//                    try {
//                        pageRepository.save(page);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        System.out.println(path);
//                        return;
//                    }

                }
            }

        } else {
            //Если ссылка есть в cash - то не следует ничего делать!
            return;
//            code = cache.get(path).getCode();
//            System.out.format(" --- from cache: [%d]  %s\n", code, path);
//            String html = cache.get(path).getContent();
//            document = Jsoup.parseBodyFragment(html);
        }

        //Запись в базу данных по одной странице
        //pageRepository.save(page);

        List<Parser> taskList = new ArrayList<>();
        Set<String> hReference = HtmlParsing.getAllLinks(document, domainName);
        //TreeSet<String> hReference = HtmlParsing.getAllLinksExt(document);

        //Здесь нужна проверка на наличие ссылки в уже обработанных - дабы исключить
        //повторные попытки

        if (hReference != null)
            for (String hRef : hReference) {
                if (!inProgressListSet.contains(hRef))
                    if ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!skipListSet.contains(hRef))) {
                        Parser parser = new Parser(siteId, hRef, domainName);
                        taskList.add(parser);
                        parser.fork();
                    }
            }
        //String returnStr = "Insert into Page (code,path,content) values (?,?,?)\n";
        for (Parser task : taskList) {
            task.join();
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


    private void savePagesFromCash() {
        TimeMeasure.setStartTime();
        System.out.println("Запись в базу:");
        Parser.getCache().entrySet().forEach(p -> {
            if (p.getValue().getCode() != null) {
                pageRepository.save(p.getValue());
            }
        });
        System.out.format("Страницы сайта загружены за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));
    }

}
