package engine.service;

import engine.repository.PageRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class Parser1 extends Thread {

    private String url;
    PageRepository pageRepository;

    @Override
    public void run()  {
        String saveDirectory = "data/" + HtmlParsing.getDomainName(url);
        String fileName = saveDirectory.concat("/heap.obj");

        try {
            Files.createDirectories(Paths.get(saveDirectory));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Запускаем парсинг
        SiteMap siteMap = new SiteMap(fileName, pageRepository);
        siteMap.invokeForkJoinPool(url);

        List<String> site = siteMap.getSiteMap(url);
        //Карта сайта - вывод в консоль
        //site.forEach(System.out::println);

        try {
            Files.write(Path.of("data/" + HtmlParsing.getDomainName(url) + ".site"),site);
            System.out.format("Карта сайта %s создана.", url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.format("Проверка количества страниц: %d\n",siteMap.getHeap().size());
        //siteMap.getHeap().entrySet().forEach(l-> System.out.println(l.getKey() + " : " + l.getValue().getStatusCode() + " , Length: " + l.getValue().getHtml().length()));

    }




}
