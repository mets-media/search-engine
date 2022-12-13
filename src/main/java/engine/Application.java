package engine;

import engine.config.YAMLConfig;
import engine.entity.Site;
import engine.repository.ConfigRepository;
import engine.repository.FieldRepository;
import engine.repository.PageContainerRepository;
import engine.repository.PartOfSpeechRepository;
import engine.repository.SiteRepository;
import engine.service.HtmlParsing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    @Autowired
    private YAMLConfig yamlConfig;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner dataGenerator(ConfigRepository configRepository,
                                           PartOfSpeechRepository partOfSpeechRepository,
                                           FieldRepository fieldRepository,
                                           PageContainerRepository pageContainerRepository) {
        return args -> {

            //dataSource();

            HtmlParsing.setUserAgent(yamlConfig.getUserAgent());
            HtmlParsing.setReferrer(yamlConfig.getReferrer());
            HtmlParsing.setTimeout(yamlConfig.getTimeout());
            List<Site> siteList = yamlConfig.getSites();

            if (fieldRepository.count() == 0)
                fieldRepository.initData();

            if (configRepository.count() == 0)
                configRepository.initData();

            if (partOfSpeechRepository.count() == 0)
               partOfSpeechRepository.initData();

             pageContainerRepository.createFunction();

             try {
                 pageContainerRepository.createTrigger();
             } catch (Exception e) {

             }

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
