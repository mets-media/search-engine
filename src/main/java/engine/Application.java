package engine;

import engine.repository.ConfigRepository;
import engine.repository.FieldRepository;
import engine.repository.PageContainerRepository;
import engine.repository.PartOfSpeechRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner dataGenerator(ConfigRepository configRepository,
                                           PartOfSpeechRepository partOfSpeechRepository,
                                           FieldRepository fieldRepository,
                                           PageContainerRepository pageContainerRepository) {
        return args -> {
            if (configRepository.count() == 0)
                configRepository.initData();

            if (partOfSpeechRepository.count() == 0)
               partOfSpeechRepository.initData();

            if (fieldRepository.count() == 0)
                fieldRepository.initData();

             pageContainerRepository.createFunction();
             pageContainerRepository.createTrigger();
        };
    }


}
