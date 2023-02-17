package engine;

import engine.config.YAMLConfig;
import engine.repository.*;
import engine.service.ApiService;
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
                                           LemmaRepository lemmaRepository,
                                           IndexRepository indexRepository,
                                           PartOfSpeechRepository partOfSpeechRepository,
                                           FieldRepository fieldRepository,
                                           PageContainerRepository pageContainerRepository) {
        return args -> {

            HtmlParsing.setUserAgent(yamlConfig.getUserAgent());
            HtmlParsing.setReferrer(yamlConfig.getReferrer());
            HtmlParsing.setTimeout(yamlConfig.getTimeout());

            //------- Инициализация служебных данных ------------
            if (fieldRepository.count() == 0)
                fieldRepository.initData();
            if (configRepository.count() == 0)
                configRepository.initData();
            if (partOfSpeechRepository.count() == 0)
               partOfSpeechRepository.initData();
            //----------------------------------------------------
            configRepository.createOneRecordTable();

            configRepository.createTriggers();
            configRepository.createTrigger();
            lemmaRepository.createLemmaTrigger();
            indexRepository.createIndexTrigger();
            //Парсинг
            pageContainerRepository.createTrigger();
            //Запрос лемм по всем сайтам
            configRepository.createFunctionForAllSiteLemmaInfo();
            configRepository.createFunctionResetCounters();
            configRepository.createGetPagesFunction();
            configRepository.createGetByLemmaAndSiteIdFunction();
            indexRepository.createForeignKeys();
            //счётчики удалений
            configRepository.createSequences();

            configRepository.creteGetCountersFunction();
        };
    }
}
