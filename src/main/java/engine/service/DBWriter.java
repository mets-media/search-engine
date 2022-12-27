package engine.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import engine.entity.Page;
import engine.entity.PartsOfSpeech;
import engine.view.CreateUI;
import engine.view.SiteComponent;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class DBWriter extends Thread {
    private final BeanAccess beanAccess;


    private final ConcurrentLinkedQueue<Page> readyPage;
    private final Integer batchSize;
    private final boolean checkPartOfSpeech;
    private final List<String> listLemmaString = new ArrayList<>();
    private final Lemmatization lemmatizator;
    private final List<Page> preparePages = new ArrayList<>();
    private final List<String> lemmaStrings = new ArrayList<>();

    private final List<Page> errorInsertPage = new ArrayList<>();
    private final List<String> errorLemmaString = new ArrayList<>();


    private boolean run;


    public DBWriter(String name, BeanAccess beanAccess, ConcurrentLinkedQueue<Page> readyPage, Integer batchSize, boolean checkPartOfSpeech) {
        super(name);
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
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

//          Новый вариант - с заранее подготовленными строками лемматизации
            readyPage.stream().findFirst().ifPresent(page -> {
                if (page.getPath().length() > 255) {
                    //Записать длинную ссылку
                    //longLinkRepository
                }
                //Исключение ошибки 0x00 UTF-8
                page.setContent(page.getContent().replaceAll("\u0000", ""));

                preparePages.add(page);
                lemmaStrings.add(getLemmaString(page.getContent(), lemmatizator));
                readyPage.remove(page); //Удаление обработанной страницы
            });

            if (preparePages.size() >= batchSize) {
                int[] results = new int[0];
                try {
                    results = batchUpdate(preparePages, lemmaStrings);
                } catch (Exception e) {// Возникновении ошибки - транзакция откатывается => записываем по одной...
                    //throw new RuntimeException(e);
                    e.printStackTrace();
                    System.out.println(getName() + " Ошибка записи данных! => записываем без общей транзакции");
                    //Определяем в каком Insert ошибка

                    Arrays.stream(results).dropWhile(i -> (i == 1)).forEach(i -> {
                        errorInsertPage.add(preparePages.get(i));
                        errorLemmaString.add(lemmaStrings.get(i));

                        System.out.println("Страниц с ошибками: " + errorInsertPage.size());

                        preparePages.remove(i);
                        lemmaStrings.remove(i);
                    });

                    //Повторная попытка записи произойдёт в следующем цикле

                } finally {

                    int siteId = preparePages.get(0).getSiteId();
                    int pageCount = beanAccess.getPageRepository().countBySiteId(siteId);
                    beanAccess.getSiteRepository().setPageCountBySiteId(siteId, pageCount);

                    System.out.printf("Общее число старанниц в базе данных: %d страниц\n", beanAccess.getPageRepository().count());

                    preparePages.clear();
                }
            }


//            if (readyPage.size() > batchSize) {
//                List<Page> savePage = readyPage.stream().limit(batchSize).toList();
//
//                try {
//                    batchUpdate(savePage);
//                } catch (Exception e) {
//                    //e.printStackTrace();
//                }
//
//                readyPage.removeAll(savePage);
//                System.out.printf("Общее число старанниц в базе данных: %d страниц\n", beanAccess.getPageRepository().count());
//            }


        }//========================================================================================================

        //Прерывание бесконечного цикла по run = false
        //List<Page> savePage = readyPage.stream().toList();
        //batchUpdate(savePage);
        //readyPage.removeAll(savePage);

        batchUpdate(preparePages, lemmaStrings);
        System.out.println("Запись хвоста: " + beanAccess.getPageRepository().count());

        System.out.printf("Общее число старанниц в базе данных: %d страниц\n", beanAccess.getPageRepository().count());

        this.interrupt();
        System.out.println(getName() + " остановлен!");
    }

    public void stopWriter() {
        run = false;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean batchUpdate(List<Page> savePages) {

        System.out.println("Запись данных; name = " + getName());

        String sql = "Insert into Page_Container (Site_Id, Code, Path, Content, Lemmatization) values (?,?,?,?,?)";
        //int[] results = beanAccess.getJdbcTemplate().batchUpdate(sql, new InterruptibleBatchPreparedStatementSetter() {
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
                    path = "Truncate link:  (" + Math.random() + "): " + path.substring(0, 200);
                }
                ps.setString(3, path);
                String content = page.getContent();
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

//            @Override
//            public boolean isBatchExhausted(int i) {
//                if (i < batchSize) return true;
//                else return false;
//            }

        });

        for (int i : results) {
            //System.out.print(i);
            if (i == 0) return false;
        }

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
            System.out.println("!!!!!!!!!!!!!!!!!!!!!  lemmaString = null  !!!!!!!!!!!!!!!!!!!!!!!!");

        }

        return totalString;
    }
}
