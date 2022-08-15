package engine.service;

import engine.entity.Page;
import engine.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
//public class Parser extends RecursiveTask<String> {
public class Parser extends RecursiveAction {
    private long id;
    private long idParent;
    private String path;
    private Integer code;
    private String content;
    private String domainName;
    private static ConcurrentHashMap<String, Page> cache = new ConcurrentHashMap<>();
    private AtomicLong idSequence = new AtomicLong();

    private static PageRepository pageRepository;
    private static volatile boolean active;
    private static volatile boolean cacheModify = false;
    public static boolean isCacheModify() {
        return cacheModify;
    }
    public static ConcurrentHashMap<String, Page> getCache() {
        return cache;
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
    public Parser(String path, String domainName) {
        this.path = path;
        this.domainName = domainName;
    }
    public static void start(String path) {

        pageRepository.deleteAll();

        String domainName = HtmlParsing.getDomainName(path);
        String fileName = "data/" + domainName + "/cache.obj";

        loadCache(fileName);
        active = true;

        TimeMeasure.setStartTime();
        Parser parser = new Parser(path, domainName);
        parser.invoke();

        System.out.println("Завершение потоков!");

        System.out.format("Страницы сайта загружены за %s\n",  TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        //Запись Cache.obj
        TimeMeasure.setStartTime();
        saveCache(fileName);
        System.out.format("Cache.obj записан за %s\n",  TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        TimeMeasure.setStartTime();
        System.out.println("Запись в базу:");
        Parser.getCache().entrySet().forEach(p -> {
            if (p.getValue().getCode() != null) {
                pageRepository.save(p.getValue());
            }
        });
        System.out.format("Страницы сайта загружены за %s\n",  TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

    }

    @Override
    //protected String compute() {
    protected void compute() {
        id = idSequence.incrementAndGet();
        Document document = null;
        Page page = null;

        if (!cache.containsKey(path)) {
            try {
                code = HtmlParsing.getStatusCode(path);
                System.out.format("download: [%d]  %s\n", code, path);
                document = HtmlParsing.getHtmlDocument(path);
                content = document.body().toString();
                cacheModify = true;
            } catch (Exception e) {
                //throw new RuntimeException(e);
                //e.printStackTrace();
                content = "";
                code = HtmlParsing.getStatusFromExceptionString(e.toString());
                System.out.println("Exception! :" + e);
            }
            if (code != null) { //Таймаут сервера и т.п.
                //page = new Page(id, idParent, path, code, content);
                page = new Page(path, code, content);
                if (code == 200) {
                    cache.put(path, page);
                }
            }

        } else {
            code = cache.get(path).getCode();
            System.out.format(" --- from cache: [%d]  %s\n", code, path);
            String html = cache.get(path).getContent();
            document = Jsoup.parseBodyFragment(html);
        }

        //Запись в базу данных по одной странице
        //pageRepository.save(page);

        List<Parser> taskList = new ArrayList<>();
        List<String> hReference = HtmlParsing.getAllLinks(document);

        if (hReference != null)
        for (String hRef : hReference) {
            //if ((hRef.toLowerCase().indexOf(domainName) >= 0) && (!cache.containsKey(hRef))) {
            if ((HtmlParsing.isCurrentSite(hRef,domainName)) && (!cache.containsKey(hRef))) {
                Parser parser = new Parser(hRef, domainName);
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
                System.out.format("Страниц в файле %d\n", cache.size());
                System.out.format("Cache.obj загружен за %d сек.\n",  TimeMeasure.getExperienceTime() / 1000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void saveCache(String fileName) {
        if (!isCacheModify()) {
            System.out.println("cache.obj не обновлялся.Запись не требуется.\n");
            return;
        }

        String saveDirectory = fileName.replace("/cache.obj", "");

        try {
            Files.createDirectories(Paths.get(saveDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try { //Сохранение заполненного Heap
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            //objectOutputStream.writeObject(this.getHeap());
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
