package engine.service;

import engine.repository.PageRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class ExtractLinksTask extends RecursiveTask<String> {
    private static AtomicInteger forkCount = new AtomicInteger();
    private int delay;
    private String domainName;
    private String parent;
    private String url;
    private ConcurrentHashMap<String, Link> heap = new ConcurrentHashMap<>();

    PageRepository pageRepository;


    public ExtractLinksTask(String parent, String url, ConcurrentHashMap<String, Link> heap, PageRepository pageRepository, int delay, String domainName) {
        this.delay = delay;
        this.domainName = domainName;
        this.parent = parent;
        this.url = url;
        this.heap = heap;
        this.pageRepository = pageRepository;
    }


    @Override
    protected String compute() {

        System.out.println(forkCount.incrementAndGet());
        System.out.println("Fork: " + url);

        List<ExtractLinksTask> taskList = new ArrayList<>();
        List<String> links = null;

        String str = parent + " - " + url;

        Integer statusCode = null;
        Document doc = null;

        try {
            if (!heap.containsKey(url)) {
                sleep(delay);
                statusCode = HtmlParsing.getStatusCode(url);
                if (statusCode == 200) {
                    doc = HtmlParsing.getHtmlDocument(url);
                    heap.put(url, new Link(parent, doc.body().toString(), statusCode));
                }
            } else {
                statusCode = heap.get(url).getStatusCode();
                String html = heap.get(url).getHtml();
                doc = Jsoup.parseBodyFragment(html);
                }

        } catch (Exception e) {
            String exceptString = e.toString();
            heap.put(HtmlParsing.getUrlFromExceptionString(exceptString),
                    new Link(parent, null, HtmlParsing.getStatusFromExceptionString(exceptString)));
            System.out.println("Exception! :" + e);
            //e.printStackTrace();
        }

        //Запись в базу данных
        pageRepository.save(new engine.entity.Page(url,statusCode,doc.body().toString()));

        links = HtmlParsing.getAllLinks(doc);
        for (String l : links) {
            if ((l.toLowerCase().indexOf(domainName) >= 0) && (!heap.containsKey(l))) {
                ExtractLinksTask extractLinksTask = new ExtractLinksTask(url, l, heap, pageRepository, delay, domainName);
                taskList.add(extractLinksTask);
                extractLinksTask.fork();
            }
        }
        for (ExtractLinksTask task : taskList) {
            str = task.join();
        }

        return str;
    }

}


