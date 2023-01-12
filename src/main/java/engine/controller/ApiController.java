package engine.controller;

import engine.entity.Page;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import engine.service.BeanAccess;
import engine.service.Parser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    BeanAccess beanAccess;

    @RequestMapping(value = "/admin", method = RequestMethod.GET) // ищет файл index.html в resources/templates
    public String index() {
        return "index.html";
    }

    @PostMapping(value ="/indexPage",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> indexPage(@RequestParam String path) {
        beanAccess.getPageRepository().deleteByPath(path);

        return ResponseEntity.ok("{\n" +
                "'result': true\n" +
                "}");

    }

    @GetMapping(value = "/stopIndexing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> StopIndexing() {
        List<Site> listSites = beanAccess.getSiteRepository().findAll();

        if (!(listSites.get(0).getStatus() == SiteStatus.INDEXING)) {
            return ResponseEntity.ok("{\n" +
                    "'result': false,\n" +
                    "'error': \"Индексация не запущена\"\n" +
                    "}");
        }

        listSites.forEach(site -> {
            if (site.getStatus().equals(SiteStatus.INDEXING)) {
                Parser.stop(site);
                site.setStatus(SiteStatus.STOPPED);
                site.setStatusTime(LocalDateTime.now());
                beanAccess.getSiteRepository().save(site);
                site.setPageCount(beanAccess.getPageRepository().countBySiteId(site.getId()));
                beanAccess.getSiteRepository().save(site);
            }
        });


        return ResponseEntity.ok("{\n" +
                "'result': true\n" +
                "}");
    }


    @GetMapping(value = "/startIndexing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> StartIndexing() {

        List<Site> listSites = beanAccess.getSiteRepository().findAll();

        if (listSites.get(0).getStatus() == SiteStatus.INDEXING) {
            return ResponseEntity.ok("{\n" +
                    "'result': false,\n" +
                    "'error': \"Индексация уже запущена\"\n" +
                    "}");
        }

        beanAccess.getPageRepository().reCreateTable();
        beanAccess.getLemmaRepository().reCreateTable();
        beanAccess.getIndexRepository().reCreateTable();
        beanAccess.getKeepLinkRepository().reCreateTable();


        for (Site site : listSites) {
            site.setPageCount(0);
            site.setStatus(SiteStatus.NEW_SITE);
            beanAccess.getSiteRepository().save(site);
        }

        Parser.setDataAccess(beanAccess);

        listSites.forEach(site -> {
            Parser.getStopList().remove(site);
            site.setStatus(SiteStatus.INDEXING);
            beanAccess.getSiteRepository().save(site);
            Parser.start(site);
        });

        return ResponseEntity.ok("{\n" +
                "'result': true\n" +
                "}");
    }


}
