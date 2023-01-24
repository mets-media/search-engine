package engine.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import engine.config.YAMLConfig;
import engine.controller.ApiController;
import engine.repository.*;
import engine.view.SiteComponent;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
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
    StatusRepository statusRepository;
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

    }
}

