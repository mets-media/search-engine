package engine.service;

import engine.controller.response.ResponseError;
import engine.controller.response.ResponseOk;
import engine.controller.response.ResponseSearch;
import engine.controller.response.Statistics;
import engine.dto.*;
import engine.entity.Lemma;
import engine.entity.Page;
import engine.entity.PathTable;
import engine.entity.Site;
import engine.enums.SiteStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ApiService {
    private static BeanAccess beanAccess;
    private static Lemmatization lemmatizator;

    public static void setBeanAccess(BeanAccess beanAccess) {
        ApiService.beanAccess = beanAccess;
    }

    public static ResponseEntity<?> getSites() {
        List<Site> list = beanAccess.getSiteRepository().findAll();
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    public static ResponseEntity<?> search(QueryDto queryDto) {

        String requestStr = queryDto.query();
        AtomicReference<Integer> siteId = new AtomicReference<>();
        siteId.set(-1);

        if (queryDto.site().equals("*"))
            siteId.set(0);  //все сайты
        else
            beanAccess.getSiteRepository().getSiteByUrl(queryDto.site()).ifPresent(site -> siteId.set(site.getId()));

        if (siteId.get() == -1)
            return new ResponseEntity<>(new ResponseError("Сайт не найден"),
                    HttpStatus.NOT_FOUND);

        if (requestStr.isBlank()) {
            return new ResponseEntity<>(new ResponseError("Запрос не может быть пустым"),
                    HttpStatus.BAD_REQUEST);
        }

        if (HtmlParsing.getRussianListString(requestStr).size() == 0) {
            return new ResponseEntity<>(new ResponseError("Запрос должен содержать русские слова!"),
                    HttpStatus.BAD_REQUEST);
        }

        Lemmatization.setBeanAccess(beanAccess);
        if (lemmatizator == null)
            lemmatizator = Lemmatization.getLemmatizator();

        //Ищем леммы
        HashMap<String, Integer> requestLemmas = lemmatizator.getLemmaHashMap(requestStr);

        if (requestLemmas.size() == 0) {
            return new ResponseEntity<>(new ResponseError("Лемматизатор не распознал леммы. Проверьте запрос."),
                    HttpStatus.NOT_FOUND);
        }

        String includeLemma = requestLemmas
                .keySet()
                .stream()
                .collect(Collectors.joining("','", "'", "'"));

        List<Lemma> lemmaList;
        if (siteId.get() == 0) { //Все сайты
            lemmaList = beanAccess.getImplRepository().findLemmasInAllSites(includeLemma);
        } else  //Выбранный сайт
            lemmaList = beanAccess.getLemmaRepository().findBySiteIdAndLemmaIn(siteId.get(),
                    requestLemmas.keySet().stream().toList());

        if (lemmaList.size() == 0)
            return new ResponseEntity<>(new ResponseError("Леммы для вашего запроса не найдены на страницах сайтов"),
                    HttpStatus.NOT_FOUND);

        Set<Lemma> lemmaSet = new HashSet<>(lemmaList);

        var findPages = findPages(lemmaSet, siteId.get());

        var result = getFullInfo(findPages, lemmaSet, lemmatizator);

        if (result.size() == 0)
            return new ResponseEntity<>(new ResponseError("Страницы с комбинацией переданных лемм не найдены. " +
                    "Сократите запрос."), HttpStatus.NOT_FOUND);

        int resultsCount = result.size();
        int offset = queryDto.offset();
        int limit = queryDto.limit();

        if ((offset < 0) || (offset > (result.size() - 1)))
            return new ResponseEntity<>(new ResponseError("Offset за допустимыми пределами! " +
                    "Найдено страниц: " + resultsCount),
                    HttpStatus.NOT_FOUND);

        int toIndex = offset + limit;

        if (toIndex >= resultsCount)
            toIndex = resultsCount;

        result = result.subList(offset, toIndex);

        return new ResponseEntity<>(new ResponseSearch(resultsCount, result), HttpStatus.OK);
    }//-------------------------------------------------------------

    static String getTitle(String content) {
        int start = content.indexOf("<title>") + 7;
        int end = content.indexOf("</title>");
        if (start > 0)
            return content.substring(start, end);
        else {
            return "";
        }
    }

    private static Boolean lemmaIsPresent(HashMap<String, Integer> findLemmas, Set<String> lemmas) {
        for (String lemma : findLemmas.keySet()) {
            if (lemmas.contains(lemma))
                return true;
        }
        return false;
    }

    public static Set<String> getSnippetsFragment(String text, Set<Lemma> lemmaSet, Lemmatization lemmatizator) {
        Set<String> words = new HashSet<>();

        String[] russianWords = HtmlParsing.getRussianWords(text);
        List<String> listWord = new ArrayList<>(Arrays.stream(russianWords).toList());

        listWord.sort(Comparator.comparingInt(String::length).reversed());
        Set<String> lemmas = lemmaSet.stream().map(Lemma::getLemma).collect(Collectors.toSet());

        for (String word : listWord) {

            //леммы для каждого слова
            var findLemma = lemmatizator.getLemmaHashMap(word);

            if (lemmaIsPresent(findLemma, lemmas))
                text = text.toLowerCase()
                        .replaceAll("[^<b>]" + word + "[^</b>]", " <b>" + word + "</b> ");

            int i = text.indexOf("<b>" + word + "</b>");

            if (i > 0) {
                i -= 25;
                int j = i + word.length() + 50;
                if (i < 0) i = 0;
                if (j > text.length()) j = text.length();

                words.add(trimToSpaces(text.substring(i, j)));
            }
        }
        return words;
    }

    private static String trimToSpaces(String text) {
        return text.substring(text.indexOf(" "),text.lastIndexOf(" "));

    }
    public static String getSnippetHtmlDocument(String text, Set<Lemma> lemmaSet, Lemmatization lemmatizator) {

        Set<String> words = getSnippetsFragment(text, lemmaSet, lemmatizator);

        String result = "";
        for (String word : words) {
            result = result.concat("<p> ... " + word + " ... ; </p>\n");
        }

        String htmlString = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <title>Snippet</title>
                </head>
                <body>
                :fragmentsWithLemma
                </body>
                </html>""";

        return htmlString.replace(":fragmentsWithLemma", result);
    }

    static String getSnippet(String content, Set<Lemma> lemmaSet, Lemmatization lemmatizator) {

        var listHtml = HtmlParsing.getHTMLStringsContainsLemma(content, lemmaSet, lemmatizator);

        return ApiService.getSnippetHtmlDocument(listHtml.stream().collect(Collectors.joining(" ")),
                        lemmaSet, lemmatizator);
    }

    static List<FindPageDto> getFullInfo(List<PathTable> pathTableList,
                                         Set<Lemma> lemmaSet, Lemmatization lemmatizator) {

        List<FindPageDto> result = new ArrayList<>();

        for (PathTable pathTable : pathTableList) {

            Page page = beanAccess.getPageRepository().getById(pathTable.getPageId());
            Integer siteId = page.getSiteId();
            beanAccess.getSiteRepository().findById(siteId).ifPresent(site ->
                    result.add(new FindPageDto
                            (
                                    site.getUrl(),
                                    site.getName(),
                                    pathTable.getPath(),
                                    getTitle(page.getContent()),
                                    getSnippet(page.getContent(), lemmaSet, lemmatizator),
                                    pathTable.getRelRelevance()
                            )
                    ));
        }
        return result;
    }

    private static List<PathTable> findPages(Set<Lemma> selectedLemmas, int siteId) {
        String resultSQL;

        if (siteId == 0) { // Все сайты
            resultSQL = SearchService.getGeneratedSQL(selectedLemmas);
        } else { //Выбранный сайт
            String pageIdArray;
            String lemmaIdArray;

            HashMap<String, List<Integer>> pageIdHashMap = new HashMap<>();

            //----------------------------------------------------------------------------------
            List<Integer> listPagesId = SearchService.getCommonPages(selectedLemmas, siteId, pageIdHashMap);

            /** Формируем строку с перечислением Page_Id **/
            pageIdArray = listPagesId.stream().map(p -> Integer.toString(p))
                    .collect(Collectors.joining(","));

            /** Формируем строку с перечислением Lemma_Id **/
            lemmaIdArray = SearchService.getLemmaIdString(selectedLemmas, siteId);
            //----------------------------------------------------------------------------------

            if (listPagesId.size() != 0) { //Не смысла запрашивать - если страниц не существует

                resultSQL = SearchService.getSQLByName("oneSiteQuery")
                        .replace(":lemmaIdArray", lemmaIdArray)
                        .replace(":pageIdArray", pageIdArray);
            } else {
                return new ArrayList<>();
            }
        }
        return beanAccess.getImplRepository().findPathTableItems(resultSQL);
    }

    public static ResponseEntity<?> startIndexing() {
        List<Site> listSites = beanAccess.getSiteRepository().findAll();

        for (Site site : listSites) {
            if (site.getStatus() == SiteStatus.INDEXING)
                return new ResponseEntity<>(new ResponseError("Индексация уже запущена!"), HttpStatus.OK);
        }

        beanAccess.getIndexRepository().reCreateTable();
        beanAccess.getPageRepository().reCreateTable();
        beanAccess.getLemmaRepository().reCreateTable();
        beanAccess.getKeepLinkRepository().reCreateTable();
        beanAccess.getIndexRepository().createForeignKeys();
        beanAccess.getConfigRepository().resetSequences();

        for (Site site : listSites) {
            site.setPageCount(0);
            site.setStatus(SiteStatus.NEW_SITE);
            site.setLastError("");
            beanAccess.getSiteRepository().save(site);
        }

        Parser.setBeanAccess(beanAccess);

        listSites.forEach(site -> {
            Parser.getStopList().remove(site);
            site.setStatus(SiteStatus.INDEXING);
            beanAccess.getSiteRepository().save(site);
            Parser.start(site);
        });

        return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);
    }

    public static ResponseEntity<?> stopIndexing() {
        List<Site> listSites = beanAccess.getSiteRepository().findAll();

        boolean result = false;
        for (Site site : listSites) {
            if (site.getStatus() == SiteStatus.INDEXING) {
                result = true;
                break;
            }
        }
        if (!result) {
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
        SearchService.refreshSitesInformation();
        return new ResponseEntity<>(new ResponseOk(), HttpStatus.OK);
    }


    public static Statistics getStatistics() {

        List<SiteInfoDto> siteInfoDtoList = beanAccess.getSiteRepository().getTotalInfo();

        int sites = siteInfoDtoList.size();
        int pages = 0;
        int lemmas = 0;
        boolean isIndexing = true;

        Statistics statistics = new Statistics();

        for (SiteInfoDto siteInfo : siteInfoDtoList) {

            if (siteInfo.status() == SiteStatus.FAILED)
                statistics.getDetailed().add(siteInfo);
            else
                statistics.getDetailed().add(getSiteDtoWithoutError(siteInfo));

            pages += siteInfo.pages();
            lemmas += siteInfo.lemmas();
            if (siteInfo.status() != SiteStatus.INDEXED) isIndexing = false;
        }

        statistics.setTotal(new TotalDto(sites, pages, lemmas, isIndexing));

        return statistics;
    }

    private static SiteInfoWithoutErrorDto getSiteDtoWithoutError(SiteInfoDto fromDto) {
        return new SiteInfoWithoutErrorDto(
                fromDto.url(),
                fromDto.name(),
                fromDto.status(),
                fromDto.statusTime(),
                fromDto.pages(),
                fromDto.lemmas()
        );
    }
}
