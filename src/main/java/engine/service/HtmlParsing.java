package engine.service;

import engine.entity.Lemma;
import lombok.experimental.UtilityClass;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class HtmlParsing {
    private static String userAgent;
    private static String referrer;
    private static Integer timeout;

    public static void setUserAgent(String userAgent) {
        HtmlParsing.userAgent = userAgent;
    }

    public static void setReferrer(String referrer) {
        HtmlParsing.referrer = referrer;
    }

    public static void setTimeout(Integer timeout) {
        HtmlParsing.timeout = timeout;
    }


    public static String getDomainName(String url) {

        url = url.toLowerCase().replace("www.", "");
        //String regEx = "\\/\\/[^.]+.[^\\W]+";
        String regEx = "[a-z0-9_-]+(\\.[a-z0-9_-]+)*\\.[a-z]{2,5}";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return url.substring(matcher.start(),
                    matcher.end()).replace("www.", "").replace("WWW.", "");
        } else
            return null;
    }

    public static synchronized Integer getStatusCode(String url) {

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(timeout)
                    .execute();
            return response.statusCode();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized Integer getStatusCode(String url, int readTimeout) {

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .timeout(readTimeout)
                    .execute();
            return response.statusCode();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized Document getHtmlDocument(String url) throws Exception {

        return Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer)
                .timeout(timeout)
                .get();
    }

    public static synchronized Document getHtmlDocument(String url, Integer timeout) throws Exception {

        return Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer)
                .timeout(timeout)
                .get();
    }

    public static Integer getStatusFromExceptionString(String exceptString) {
        final String TIME_OUT = "Read timed out";
        if (exceptString.contains(TIME_OUT)) return -2;

        String regEx = "Status=[\\d]{3}";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(exceptString);

        if (matcher.find()) {
            return Integer.parseInt(exceptString.substring(matcher.start() + 7, matcher.end()));
        } else {
            return -1;
        }
    }

    public static boolean isCurrentSite(String hRef, String domainName) {
        String regEx = "\\W" + domainName + "\\W";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(hRef);

        return matcher.find();
    }

    private Boolean isFileLink(String hRef) {

        String RegEx = "\\.[A-z]{3,4}$";
        Pattern pattern = Pattern.compile(RegEx);
        Matcher matcher = pattern.matcher(hRef);

        return matcher.find();
    }

    public static Set<String> getAllLinks(Document document) {
        if (document == null) return null;
        Elements elements = document.select("a[href]");
        Set<String> links = new HashSet<>();

        for (Element e : elements) {
            String hRef = e.absUrl("href");

            if (!isFileLink(hRef))
                links.add(hRef);
        }
        return links;
    }

    public static String[] getRussianWords(String text) {
        String[] words = text.toLowerCase()
                .replaceAll("[^ЁёА-я]", " ")
                .trim()
                .split("[\\s+]+");
        return words;
    }


    public static List<String> getRussianListString(String content) {
        List<String> list = List.of(content.split("\n"));
        String RegEx = "[ЁёА-я]";
        Pattern pattern = Pattern.compile(RegEx);

        List<String> result = new ArrayList<>();

        for (String s : list) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.find())
                result.add(s.concat("\n"));
        }
        return result;
    }

    public static List<String> getHTMLStringsContainsLemma(String content,
                                                           Set<Lemma> searchLemmaList, Lemmatization lemmatizator) {
        List<String> result = new ArrayList<>();
        var list = getRussianListString(content);
        for (String s : list) {
            var stringLemmaHashMap = lemmatizator.getLemmaCountRankHashMap(s, 1);
            for (Lemma searchLemma : searchLemmaList) {
                if (stringLemmaHashMap.containsKey(searchLemma.getLemma()))
                    result.add(s);
            }
        }
        return result;
    }

}
