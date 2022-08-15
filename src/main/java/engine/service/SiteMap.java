package engine.service;

import engine.repository.PageRepository;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@Setter
class SiteMap implements Serializable {
    private ConcurrentHashMap<String, Link> heap = new ConcurrentHashMap<>();
    private Document document;
    private boolean isModify = false;
    private ForkJoinPool forkJoinPool;
    private String heapFileName;

    PageRepository pageRepository;

    public SiteMap(String fileName, PageRepository pageRepository) {

        this.pageRepository = pageRepository;
        heapFileName = fileName;

        int poolSize = Runtime.getRuntime().availableProcessors();
        forkJoinPool = new ForkJoinPool(poolSize);

        try {
            if (Files.exists(Paths.get(fileName))) {
                FileInputStream fileInputStream = new FileInputStream(fileName);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                heap = (ConcurrentHashMap<String, Link>) objectInputStream.readObject();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void invokeForkJoinPool(String url) {

        if (heap.size() == 0)
            try {
                Integer statusCode = HtmlParsing.getStatusCode(url);
                document = Jsoup.connect(url).get();
                heap.put(url, new Link("", document.body().toString(), statusCode));
                isModify = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        String domainName = HtmlParsing.getDomainName(url);

        ExtractLinksTask extractLinksTask = new ExtractLinksTask("",url,heap,pageRepository, 150,domainName);
        forkJoinPool.invoke(extractLinksTask);

        System.out.format("Heap.obj - сформирован. Размер %d HTML-страниц\n", heap.size());

        saveHeap(heapFileName);

        //insertIntoDataBase();
    }
    public List<String> getSiteMap(String url) {
        List<String> siteMap = new ArrayList<>();
        siteMap.add(url);
        siteMap.addAll(this.getChildFromHeap(url));
        int s = this.getHeap().size();
        for (int i = 1; i < s - 1; i++) {
            siteMap.addAll(i + 1,this.getChildFromHeap(siteMap.get(i)));
        }
        return siteMap;
    }

    public List<String> getChildFromHeap(String parentLink) {
        List<String> siteLinks = new ArrayList<>();

        String regEx = "^\t+[^\s]";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(parentLink);

        String tabChar = "";
        if (matcher.find()) {
            tabChar = parentLink.substring(0, matcher.end() - 1);
        }

        String finalTabChar = tabChar;

        heap.entrySet()
                .stream()
                .filter(l -> l.getValue()
                        .getParent()
                        .equalsIgnoreCase(parentLink.trim()))
                .collect(Collectors.toList())
                .forEach(l -> {
                    siteLinks.add(finalTabChar + "\t" + l.getKey());
                    heap.remove(parentLink.trim());
                });
        return siteLinks;
    }

    public void saveHeap(String fileName) {
        if (!isModify) {
            System.out.println("Обновлений нет.");
            return;
        }

        System.out.println("Save heap method: " + heap.size());

        try { //Сохранение заполненного Heap
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            //objectOutputStream.writeObject(this.getHeap());
            objectOutputStream.writeObject(heap);

            fileOutputStream.close();
            objectOutputStream.close();

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
