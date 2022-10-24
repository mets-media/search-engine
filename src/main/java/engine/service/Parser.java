package engine.service;

import engine.entity.Page;
import engine.repository.PageRepository;
import engine.views.Dialogs;
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
    private long parentId;
    private String path;
    private Integer code;
    private String content;
    private String domainName;
    private static ConcurrentHashMap<String, Page> cache = new ConcurrentHashMap<>();
    private static ConcurrentSkipListSet skipListSet = new ConcurrentSkipListSet();
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

    public Parser(Long parentId, String path, String domainName) {
        this.path = path;
        this.domainName = domainName;
        this.parentId = parentId;
    }

    public static void start(Long parentId, String path) {
        skipListSet.clear();

        Dialogs.showConfirmDialog("Удалить результаты предыдушего сканирования?",
                "Удалить", "Не удалять");
        if (Dialogs.getDialodResult())
            //pageRepository.deleteByParentId(parentId); //Удаление результатов предыдущего сканирования
            pageRepository.deleteByParentId(0L); //Удаление результатов предыдущего сканирования
        else {//Продолжаем сканирование? сохраняя предыдущие результаты
            List<String> links = pageRepository.findLinksByParentId(parentId);
            //skipListSet = (ConcurrentSkipListSet) pageRepository.findByParentId(parentId);
            skipListSet.add("/");
            for (int i = 1; i < links.size();i++) {
                skipListSet.add(path.concat(links.get(i)));
            }

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
        Parser parser = new Parser(parentId, path, domainName);
        parser.invoke();

        System.out.println("Завершение потоков!");

        System.out.format("Страницы сайта загружены за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        //Запись Cache.obj
        TimeMeasure.setStartTime();
        //saveCache(fileName);
        //System.out.format("Cache.obj записан за %s\n", TimeMeasure.getNormalizedTime(TimeMeasure.getExperienceTime()));

        //Зпись в базу
        //savePagesFromCash();

    }

    @Override
    //protected String compute() {
    protected void compute() {
        Document document = null;
        Page page = null;

        if (!skipListSet.contains(path)) {
            try {
                code = HtmlParsing.getStatusCode(path);
                //skipListSet.add(path);
                System.out.format("download: [%d]  %s\n", code, path);

                //Files.writeString(Paths.get("data/" + domainName + "/hRef.txt"), path, StandardCharsets.UTF_8);

                document = HtmlParsing.getHtmlDocument(path);
                content = document.body().toString();
                cacheModify = true;
            } catch (Exception e) {
                //throw new RuntimeException(e);
                //e.printStackTrace();
                content = "";
                code = HtmlParsing.getStatusFromExceptionString(e.toString());
                System.out.println("Exception! :" + e);
                System.out.println(path);
            }
            if (code != null) { //Таймаут сервера и т.п.
                String shortPath = path.substring(path.indexOf(domainName) + domainName.length());
                if ("".equals(shortPath))
                    if (path.contains(domainName))
                        shortPath = "/";
                page = new Page(parentId, shortPath, code, content);
                if (code == 200) {
                    skipListSet.add(path);
                    try {
                        pageRepository.save(page);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println(path);
                        return;
                    }

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
                if ((HtmlParsing.isCurrentSite(hRef, domainName)) && (!skipListSet.contains(hRef))) {
                    Parser parser = new Parser(parentId, hRef, domainName);
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

    private boolean isLinkExists() {
        return false;
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
