package engine.service;

import engine.entity.Page;
import engine.entity.PartsOfSpeech;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.InterruptibleBatchPreparedStatementSetter;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;


public class DBWriter extends Thread {
    private final BeanAccess beanAccess;
    private final ConcurrentLinkedQueue<Page> readyPage;
    private final Integer batchSize;
    private final boolean checkPartOfSpeech;
    private final List<String> listLemmaString = new ArrayList<>();
    private final Lemmatization lemmatizator;

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

        while (run) {

            while (readyPage.size() == 0)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            if (readyPage.size() > batchSize) {

                List<Page> savePage = readyPage.stream().limit(batchSize).toList();

                try {
                    batchUpdate(savePage);
                } catch (Exception e) {
                    //e.printStackTrace();
                }

                readyPage.removeAll(savePage);
                System.out.printf("Page содержит %d страниц\n", beanAccess.getPageRepository().count());

            }
        }

        List<Page> savePage = readyPage.stream().toList();
        batchUpdate(savePage);

        System.out.println("Запись всех загруженных страниц: " + beanAccess.getPageRepository().count());

        readyPage.removeAll(savePage);

        this.interrupt();
        System.out.println(getName() + " остановлен!");
    }

    public void stopWriter() {
        run = false;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean batchUpdate(List<Page> savePages) {

        System.out.println("siteId " + savePages.get(0).getSiteId() + ", name = " + getName());

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
                    path = path.substring(0,255);
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
        return totalString;
    }
}
