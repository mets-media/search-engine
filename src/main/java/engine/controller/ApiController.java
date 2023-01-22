package engine.controller;

import engine.dto.SiteInfoDto;
import engine.dto.TotalDto;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.service.BeanAccess;
import engine.service.Parser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import static engine.service.Parser.insertOrUpdatePage;

@RestController
@RequestMapping("/api")
@Setter
public class ApiController {
    static BeanAccess beanAccess;

    public static void setBeanAccess(BeanAccess beanAccess) {
        ApiController.beanAccess = beanAccess;
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

    @Getter
    public static class ResponseStatistics implements Serializable {
        private final boolean result = true;
        private TotalDto total;
        private List<SiteInfoDto> detailed;

        public ResponseStatistics(List<SiteInfoDto> detailed) {
            this.detailed = detailed;
            int sites = detailed.size();
            int pages = 0;
            int lemmas = 0;
            boolean isIndexing = true;
            for (SiteInfoDto siteInfo : detailed) {
                pages += siteInfo.pages();
                lemmas += siteInfo.lemmas();
                if (siteInfo.status() != SiteStatus.INDEXED) isIndexing = false;
            }
            this.total = new TotalDto(sites,pages,lemmas,isIndexing);
        }

    }

    @RequestMapping(value = "/admin", method = RequestMethod.GET) // ищет файл index.html в resources/templates
    public String index() {
        return "index.html";
    }

    @GetMapping(value = "/statistics")
    public ResponseEntity<?> getStatistics() {

        List<SiteInfoDto> siteInfoDtoList = beanAccess.getSiteRepository().getTotalInfo();
        ResponseStatistics statistics = new ResponseStatistics(siteInfoDtoList);

        return new ResponseEntity<>(statistics, HttpStatus.OK);
        //return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);
    }

    @PostMapping(value = "/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {

        if (!(insertOrUpdatePage(url, beanAccess))) {
            return new ResponseEntity<>(new ResponseError("Страница: " + url + " - nнаходится за пределами проиндексированных сайтов!"), HttpStatus.OK);
        }
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
        new Thread(() -> { //Обновление информации по сайтам
            for (Site site : beanAccess.getSiteRepository().getStatistic()) {
                beanAccess.getSiteRepository().save(site);
            }
        });
        return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);
    }

    @GetMapping("/test")
    public ResponseEntity<?> test(Long id) {
        return new ResponseEntity<>(new ResponseError("Индексация уже запущена!"), HttpStatus.OK);
    }

    @GetMapping(value = "/startIndexing")
    public ResponseEntity<?> StartIndexing() {
        List<Site> listSites = beanAccess.getSiteRepository().findAll();

        if (listSites.get(0).getStatus() == SiteStatus.INDEXING) {
            return new ResponseEntity<>(new ResponseError("Индексация уже запущена!"), HttpStatus.OK);
        }

        beanAccess.getIndexRepository().reCreateTable();
        beanAccess.getPageRepository().reCreateTable();
        beanAccess.getLemmaRepository().reCreateTable();
        beanAccess.getKeepLinkRepository().reCreateTable();
        beanAccess.getIndexRepository().createForeignKeys();
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


}
