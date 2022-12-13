package engine.service;

import engine.entity.Lemma;
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
import java.util.stream.Stream;

public class HtmlParsing {

    private static int saveFilesCount = 0;

    public static int getSaveFilesCount() {
        return saveFilesCount;
    }

    public static String getShortLink(String link, String domainName) {
        String shortLink = link.substring(link.indexOf(domainName) + domainName.length());
        if ("".equals(shortLink))
            if (link.contains("//".concat(domainName)))
                shortLink = "/";
            else
                shortLink = link;

        if ("/".equals(shortLink)) {
            if (link.contains(".".concat(domainName)))
                shortLink = link;
        }
        return shortLink;
    }

    public static void saveFileFromUrl(String urlFile, String filePath) throws IOException {
        filePath = filePath.concat(urlFile.substring(urlFile.lastIndexOf("/")));

        InputStream inputStream = new URL(urlFile).openStream();
        Files.copy(inputStream, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        saveFilesCount++;
        System.out.printf("\nЗаписан файл: %s", filePath);
    }

    public static String getImageUrl(Element element) {
        String regex = "src=\"https:[^\"]+\"";
        String eString = element.toString();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(eString);

        if (matcher.find()) {
            return eString.substring(matcher.start() + 5, matcher.end() - 1);
        } else {
            return "";
        }
    }

    public static String getDomainName(String url) {

        url = url.toLowerCase().replace("www.", "");
        //String regEx = "\\/\\/[^.]+.[^\\W]+";
        String regEx = "[a-z0-9_-]+(\\.[a-z0-9_-]+)*\\.[a-z]{2,5}";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return url.substring(matcher.start(), matcher.end()).replace("www.", "").replace("WWW.", "");
        } else
            return null;
    }

    public static Integer getStatusCode(String url) {

        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Chrome/100.0.4896.127")
                    .timeout(10000)
                    .execute();
            return response.statusCode();

        } catch (IOException e) {
            //return getStatusFromExceptionString(e.toString());
            throw new RuntimeException(e);
        }
    }

    public static synchronized Document getHtmlDocument(String url) throws Exception {
        Document document = Jsoup.connect(url)
                .userAgent("Chrome/100.0.4896.127")
                .referrer("http://www.google.com")
                //.ignoreContentType(true)
                .get();
        return document;

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
        //return (hRef.toLowerCase().indexOf("://" + domainName) >= 0) ? true : false;

        String regEx = "\\W" + domainName + "\\W";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(hRef);

        return matcher.find();
    }

    public static Document getHtmlDocumentFromFile(Path filePath) {
        String htmlString = null;
        try {
            htmlString = Files.readString(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Document document = Jsoup.parseBodyFragment(htmlString);
        return document;
    }

    public static Elements getElements(Document htmlDocument, String cssQuery) {
        return htmlDocument.select(cssQuery);
    }

    public static Set<String> getAllLinks(Document document, String domainName) {
        if (document == null) return null;
        Elements elements = document.select("a[href]");
        Set<String> links = new HashSet<>();

        for (Element e : elements) {
            String hRef = e.absUrl("href");

            String RegEx = "\\.[A-z]{3,4}$";
            Pattern pattern = Pattern.compile(RegEx);
            Matcher matcher = pattern.matcher(hRef);
            if (matcher.find())
                hRef = "";

            if (hRef.length() > 4) {
                //if (".jpg".equalsIgnoreCase(hRef.substring(hRef.length() - 4))) {
                if (hRef.contains(".jpg") || hRef.contains(".JPG")) {
                    hRef = "";
                }
                //if (".pdf".equalsIgnoreCase(hRef.substring(hRef.length() - 4))) {
                if (hRef.contains(".pdf") || hRef.contains(".PDF")) {
                    hRef = "";
                }


            }

            if (!"".equals(hRef)) {
                links.add(hRef);
            }
        }
        return links;
    }

    public static Set<String> getAllTitles(String content) {

        Set<String> titles = new TreeSet<>();

        Document document = Jsoup.parseBodyFragment(content);

        //Pattern pattern = Pattern.compile("\\Wtitle\\W");

        document.getAllElements().forEach(element -> {
            element.attributes().forEach(attr -> {
                if (!(attr.getValue() == null))
                    if (attr.getValue().contains("title")) {
                        //if (pattern.matcher(attr.getValue()).find()) {
                        String[] strings = element.toString().split(">");

                        String titleString = element.toString();
//                        if (strings.length >= 2) {
//                            titleString = strings[1].substring(0, strings[1].indexOf("<")).trim();
//                        }

                        if (!titleString.isBlank())
                            titles.add(titleString + '\n');

                    }
            });
        });
        return titles;
    }

    public static String[] getRussianWords(String text) {
        String[] words = text.toLowerCase()
                .replaceAll("[^\\p{IsCyrillic}]", " ")
                .trim()
                .split("[\\s+]+");
        return words;
    }

    public static String getTagBody(Element element) {
        String resultStr = element.toString();
        int posForwardBracket = resultStr.substring(0, resultStr.lastIndexOf(">") - 1).lastIndexOf(">");
        int posBackwardBracket = resultStr.lastIndexOf("<");
        resultStr = resultStr.substring(posForwardBracket + 1, posBackwardBracket);
        return resultStr.trim();
    }

    public static List<Element> getHtmlElementsByRegEx(String regEx, String content) {

        List<Element> resultSet = new ArrayList<>();
        Document document = Jsoup.parseBodyFragment(content);

        Pattern pattern = Pattern.compile(regEx);

        document.getAllElements().forEach(element -> {
            element.attributes().forEach(attr -> {
                if (!(attr.getValue() == null))
                    if (pattern.matcher(attr.getValue()).find()) {
                        //String resultString =  String.join(" ", getRussianWords(element.toString()));

//                        String resultString =  element.toString();
//                        if (!resultString.isBlank())
//                            resultSet.add(resultString);
                        resultSet.add(element);
                    }
            });
        });
        return resultSet;
    }

    public static List<String> findElementsByCss(String cssSelector, String content) {
        List<String> result = new ArrayList<>();
        Document document = Jsoup.parseBodyFragment(content);
        Elements elements = document.select(cssSelector);
        elements.forEach(element -> {
            result.add(element.toString().concat("\n\n"));
        });
        return result;
    }

    public static String getBoldRussianText(String content) {
        StringBuilder stringBuilder = new StringBuilder();

        Elements boldElements = Jsoup.parseBodyFragment(content).select("b");

        if (boldElements.size() > 0)
            boldElements.forEach(htmlString -> {
                String[] words = getRussianWords(htmlString.text());
                if (!(words[0] == ""))
                    stringBuilder.append(htmlString).append("\n...\n");
            });


        return stringBuilder.toString();
    }

}
