package engine.service;

import engine.entity.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class PageService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public List<Page> findPagesBySiteId(int siteId, int limit, int offset) {
      return entityManager.createQuery("from Page Where Site_Id = :siteId limit = :limit offset = :offset", Page.class).getResultList();


    }
}
