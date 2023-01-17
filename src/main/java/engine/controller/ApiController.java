package engine.controller;

import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.service.BeanAccess;
import engine.service.Parser;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    BeanAccess beanAccess;

    @RequestMapping(value = "/admin", method = RequestMethod.GET) // ищет файл index.html в resources/templates
    public String index() {
        return "index.html";
    }

    @PostMapping(value = "/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String path) {
        beanAccess.getPageRepository().deleteByPath(path);


        return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);

    }

    @GetMapping(value = "/stopIndexing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> StopIndexing() {
        List<Site> listSites = beanAccess.getSiteRepository().findAll();

        if (!(listSites.get(0).getStatus() == SiteStatus.INDEXING)) {
            return new ResponseEntity<>(new ResponseError("Индексация не запущена"), HttpStatus.OK);
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

        return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);
    }



    @GetMapping("/test")
    public ResponseEntity<?> test(Long id){
            return new ResponseEntity<>(new ResponseError("Индексация уже запущена!"), HttpStatus.OK);
    }


    @GetMapping(value = "/startIndexing")
    public ResponseEntity<?> StartIndexing() {
        List<Site> listSites = beanAccess.getSiteRepository().findAll();

        if (listSites.get(0).getStatus() == SiteStatus.INDEXING) {
            return new ResponseEntity<>(new ResponseError("Индексация уже запущена!"), HttpStatus.OK);
        }

        beanAccess.getPageRepository().reCreateTable();
        beanAccess.getLemmaRepository().reCreateTable();
        beanAccess.getIndexRepository().reCreateTable();
        beanAccess.getKeepLinkRepository().reCreateTable();
        //Сброс всех счётчиков
        beanAccess.getConfigRepository().resetSequences();


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

        return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);

    }

    @Getter
    @NoArgsConstructor
    public static class ResponseOk {
        private final boolean result = true;
    }

    @Getter
    public static class ResponseError {
        private final boolean result = false;
        private String error;

        public ResponseError(String error) {
            this.error = error;
        }
    }

}
