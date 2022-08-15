package engine.service;

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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HtmlParsing {

    private static int saveFilesCount = 0;

    public static int getSaveFilesCount() {
        return saveFilesCount;
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

        url = url.toLowerCase().replace("www.","");
        String regEx = "\\/\\/[^.]+.[^\\W]+";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return url.substring(matcher.start() + 2, matcher.end());
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

    public static Document getHtmlDocument(String url) throws Exception {
        Document document = Jsoup.connect(url)
                .userAgent("Chrome/100.0.4896.127")
                .referrer("http://www.google.com")
                //.ignoreContentType(true)
                .get();
        return document;

    }

    public static Integer getStatusFromExceptionString(String exceptString) {
        String regEx = "Status=[\\d]{3}";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(exceptString);

        if (matcher.find()) {
            return Integer.parseInt(exceptString.substring(matcher.start() + 7, matcher.end()));
        } else {
            return null;
        }
    }

    public static String getUrlFromExceptionString(String exceptString) {
        String regEx = "URL=[^.][^\\s]+";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(exceptString);

        if (matcher.find()) {
            return exceptString.substring(matcher.start() + 4, matcher.end());
        } else {
            return null;
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

    public static List<String> getAllLinks(Document document) {
        if (document == null) return null;
        Elements elements = document.select("a[href]");
        List<String> links = new ArrayList<>();


        Pattern pattern = Pattern.compile("http[^\"]+");
        for (Element e : elements) {
            String line = e.toString();

            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                line = line.substring(matcher.start(), matcher.end());

                if (line.charAt(line.length() - 4) != '.') {
                    links.add(line);
                }
            }
        }

        links = links.stream().distinct().collect(Collectors.toList());
//        System.out.println("--------------");
//        links.forEach(System.out::println);
//        System.out.println("--------------");
//        System.out.println("--------------");
        return links;
    }
}
