package engine.repository;

import engine.entity.Page;
import engine.entity.PartsOfSpeech;
import engine.entity.Site;
import engine.service.BeanAccess;
import engine.service.Lemmatization;
import engine.service.TimeMeasure;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class DBWriter extends Thread {
    private final BeanAccess beanAccess;
    private final Site site;
    private final ConcurrentLinkedQueue<Page> readyPage;
    private final Integer batchSize;
    private final boolean checkPartOfSpeech;
    private final Lemmatization lemmatizator;
    private boolean run;
    private boolean writeAll = false;
    public void writeAll() {
        this.writeAll = true;
    }

    public DBWriter(String name, Site site, BeanAccess beanAccess,
                    ConcurrentLinkedQueue<Page> readyPage, Integer batchSize, boolean checkPartOfSpeech) {
        super(name);
        this.site = site;
        this.beanAccess = beanAccess;
        this.readyPage = readyPage;
        this.batchSize = batchSize;
        this.checkPartOfSpeech = checkPartOfSpeech;
        lemmatizator = getNewLemmatizator();
    }

    @Override
    public void run() {
        run = true;
        System.out.println("Запуск dbWriter " + getName());

        while (run) {//===========================================================================================
            while (readyPage.size() == 0)
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            if ((readyPage.size() >= batchSize) || writeAll) {

                writeAll = false;
                List<Page> savePage = readyPage.stream().limit(batchSize).toList();

                try {
                    batchUpdate(savePage, false);
                } catch (Exception e) { //возможно ошибка utf-8 0x00
                    e.printStackTrace();
                    try {//Исключение ошибки 0x00
                        batchUpdate(savePage, true);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                readyPage.removeAll(savePage);
            }
        }//========================================================================================================

        batchUpdate(readyPage.stream().toList(), true);

        this.interrupt();
        System.out.println(getName() + " остановлен!");
    }

    public void stopWriter() {
        run = false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean batchUpdate(List<Page> savePages, boolean replace0x00) {

        TimeMeasure.setStartTime();
        String sql = "Insert into Page_Container (Site_Id, Code, Path, Content, Lemmatization) values (?,?,?,?,?)";
        int[] results = beanAccess.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Page page = savePages.get(i);

                Integer siteId = page.getSiteId();
                ps.setInt(1, siteId);
                Integer code = page.getCode();
                ps.setInt(2, code);

                String path = page.getPath();
                if (path.length() > 255) {
                    System.out.println(path);
                    System.out.printf("длина более 255 символов: %d ссылка будет сокращена до 255 ", path.length());
                    path = "Truncate link:  (" + Math.round(Math.random() * 1000) + "): " + path.substring(0, 200);
                }
                ps.setString(3, path);

                String content = page.getContent();

                if (replace0x00)//Исключение ошибки 0x00 UTF-8
                    page.setContent(page.getContent().replaceAll("\u0000", ""));

                ps.setString(4, content);

                String lemmaString = getLemmaString(content, lemmatizator);

                if (lemmaString.isEmpty())
                    System.out.println("lemmaString is empty " + page.getPath());

                ps.setString(5, lemmaString);
            }

            @Override
            public int getBatchSize() {
                return savePages.size();
            }
        });

        for (int i : results) {
            if (i == 0) return false;
        }

        System.out.println(getName() +"Страниц: " + results.length +" Время записи: "
                + TimeMeasure.getStringExperienceTime());

        return true;
    }
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private int[] batchUpdate(List<Page> preparePages, List<String> lemmaStrings) {

        System.out.println("Запись данных; name = " + getName());

        String sql = "Insert into Page_Container (Site_Id, Code, Path, Content, Lemmatization) values (?,?,?,?,?)";
        int[] results = beanAccess.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Page page = preparePages.get(i);

                Integer siteId = page.getSiteId();
                ps.setInt(1, siteId);
                Integer code = page.getCode();
                ps.setInt(2, code);

                String path = page.getPath();
                if (path.length() > 255) {
                    System.out.println(path);
                    System.out.printf("длина более 255 символов: %d ссылка будет сокращена до 255 ", path.length());
                    path = path.substring(0, 255);
                }

                ps.setString(3, path);
                String content = page.getContent();
                ps.setString(4, content);
                ps.setString(5, lemmaStrings.get(i));
            }
            @Override
            public int getBatchSize() {
                return preparePages.size();
            }
        });
        return results;
    }//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    private Lemmatization getNewLemmatizator() {
        List<String> excludeList = null;
        if (checkPartOfSpeech)
            excludeList = beanAccess.getPartOfSpeechRepository().findByInclude(false)
                    .stream()
                    .map(PartsOfSpeech::getShortName)
                    .collect(Collectors.toList());
        Lemmatization lemmatizator = new Lemmatization(excludeList, beanAccess.getFieldRepository().findByActive(true));
        return lemmatizator;
    }

    public static String getLemmaString(String content, Lemmatization lemmatizator) {

        var list =
                lemmatizator.getHashMapsLemmaForEachCssSelector(content);

        var totalInfo = lemmatizator.mergeAllHashMaps(list);

        String totalString = "";
        for (Map.Entry<String, Lemmatization.LemmaInfo> entry : totalInfo.entrySet()) {
            Lemmatization.LemmaInfo lemmaInfo = entry.getValue();
            totalString += lemmaInfo.getLemma().concat(",")
                    .concat(lemmaInfo.getCount().toString()).concat(",")
                    .concat(lemmaInfo.getRank().toString()).concat(";");
        }

        if (totalInfo == null) {
            totalString = "";
        }
        return totalString;
    }
}