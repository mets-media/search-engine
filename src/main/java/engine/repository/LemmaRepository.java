package engine.repository;

import engine.entity.Lemma;
import org.springframework.data.repository.CrudRepository;

public interface LemmaRepository extends CrudRepository<Lemma,Integer> {
}
