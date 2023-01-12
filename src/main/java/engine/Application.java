package engine;

import engine.config.YAMLConfig;
import engine.repository.*;
import engine.service.HtmlParsing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    @Autowired
    private YAMLConfig yamlConfig;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner dataGenerator(ConfigRepository configRepository,
                                           PageRepository pageRepository,
                                           PartOfSpeechRepository partOfSpeechRepository,
                                           FieldRepository fieldRepository,
                                           PageContainerRepository pageContainerRepository,
                                           SiteRepository siteRepository) {
                                           //ApplicationContext context) {
        return args -> {

            //dataSource();

            HtmlParsing.setUserAgent(yamlConfig.getUserAgent());
            HtmlParsing.setReferrer(yamlConfig.getReferrer());
            HtmlParsing.setTimeout(yamlConfig.getTimeout());
            HtmlParsing.setDelay(yamlConfig.getDelay());

            //List<Site> siteList = yamlConfig.getSites();

            //------- Инициализация служебных данных ------------
            if (fieldRepository.count() == 0)
                fieldRepository.initData();
            if (configRepository.count() == 0)
                configRepository.initData();
            if (partOfSpeechRepository.count() == 0)
               partOfSpeechRepository.initData();
            //----------------------------------------------------

            //pageRepository.createTriggers();
            //Парсинг lemmaString
            pageContainerRepository.createTrigger();
            //Функция парсинга lemmaString
            //pageContainerRepository.createFunction();

            //Удаление данных в подчинённых таблицах
            siteRepository.createTrigger();
            //siteRepository.createDeleteSiteInfoFunction();

            pageRepository.createFunctionForAllSiteLemmaInfo();
        };
    }


//    @Bean
//    @Primary
//    public DataSource dataSource() {
//        return DataSourceBuilder
//                .create()
//                .username("postgres")
//                .password("test")
//                .url("jdbc:postgresql://localhost:5432/search_engine")
//                .driverClassName("org.postgresql.Driver")
//                .build();
//    }
}
