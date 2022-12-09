package engine.repository;

import engine.entity.Lemma;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LemmaRepository extends CrudRepository<Lemma,Integer> {
    Integer countBySiteId(Integer siteId);

    List<Lemma> findByLemmaIn(List<String> lemmas, Pageable pageable);
}
