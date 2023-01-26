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

//
//    @Bean
//    public BeanNameViewResolver beanNameViewResolver(){
//        return new BeanNameViewResolver();
//    }
//
//    @Bean
//    public View admin() {
//        return new InternalResourceView("/admin/admin.html");
//    }
//
//
//    @Bean
//    public ServletRegistrationBean frontendServletBean() {
//        ServletRegistrationBean bean = new ServletRegistrationBean<>(new VaadinServlet() {
//            @Override
//            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//                if (!serveStaticOrWebJarRequest(req, resp)) {
//                    resp.sendError(404);
//                }
//            }
//        }, "/mets");
//        bean.setLoadOnStartup(1);
//        return bean;
//    }

    @Bean
    public CommandLineRunner dataGenerator(ConfigRepository configRepository,
                                           PageRepository pageRepository,
                                           LemmaRepository lemmaRepository,
                                           IndexRepository indexRepository,
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
            configRepository.createGetByLemmaAnfSiteIdFunction();

            indexRepository.createForeignKeys();
            //счётчики удалений
            configRepository.createSequences();

            configRepository.creteGetCountersFunction();
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
