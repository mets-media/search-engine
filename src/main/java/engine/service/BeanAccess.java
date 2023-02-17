package engine.service;

import engine.config.YAMLConfig;
import engine.repository.*;
import engine.view.SiteComponent;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
@Getter
public class BeanAccess {
    @Autowired
    ApplicationContext context;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    SiteRepository siteRepository;
    @Autowired
    PageRepository pageRepository;
    @Autowired
    LemmaRepository lemmaRepository;
    @Autowired
    IndexRepository indexRepository;
    @Autowired
    ConfigRepository configRepository;
    @Autowired
    FieldRepository fieldRepository;
    @Autowired
    PartOfSpeechRepository partOfSpeechRepository;
    @Autowired
    ImplRepository implRepository;
    @Autowired
    PageContainerRepository pageContainerRepository;
    @Autowired
    KeepLinkRepository keepLinkRepository;
    @Autowired
    YAMLConfig yamlConfig;
    @PersistenceContext
    EntityManager entityManager;
    @Autowired
    SiteComponent siteComponent;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    private void setPrivateVariable() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        Parser.setBeanAccess(this);
        ApiService.setBeanAccess(this);
        SearchService.setBeanAccess(this);


    }
}

