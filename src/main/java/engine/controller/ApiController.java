package engine.controller;

import engine.controller.response.ResponseError;
import engine.controller.response.ResponseOk;
import engine.dto.QueryDto;
import engine.service.ApiService;
import engine.service.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static engine.service.Parser.insertOrUpdatePage;

@RestController
@RequestMapping("/api")
public class ApiController {
    @GetMapping(value = "/statistics")
    public ResponseEntity<?> getStatistics() {
        return new ResponseEntity<>(ApiService.getStatistics(), HttpStatus.OK);
    }

    @PostMapping(value = "/indexPage")
    public ResponseEntity<?> indexPage(@RequestParam String url) {

        if (!(insertOrUpdatePage(url))) {
            return new ResponseEntity<>(new ResponseError("Страница: " + url +
                    " - находится за пределами проиндексированных сайтов!"), HttpStatus.OK);
        }
        return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);
    }

    @GetMapping(value = "/stopIndexing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stopIndexing() {
        return ApiService.stopIndexing();
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refresh() {
        SearchService.refreshSitesInformation();
        return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);
    }
    @GetMapping(value = "/startIndexing")
    public ResponseEntity<?> startIndexing() {
        return ApiService.startIndexing();
    }

    @GetMapping(value = "/search")
    public ResponseEntity<?> search(@RequestParam String query,
                                    @RequestParam Optional<String> site,
                                    @RequestParam Optional<Integer> offset,
                                    @RequestParam Optional<Integer> limit) {
        return ApiService.search(new QueryDto(query,
                                     site.orElse("*"),
                                     offset.orElse(0),
                                     limit.orElse(20)));
    }
    @GetMapping(value = "/sites")
    public ResponseEntity<?> getSites() {
        return ApiService.getSites();
    }
}
