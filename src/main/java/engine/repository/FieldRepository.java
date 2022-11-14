package engine.repository;

import engine.entity.Field;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

public interface FieldRepository extends CrudRepository<Field,Integer> {
    @Modifying
    @Transactional
    @Query(value = "Insert into Field (Name, Selector, Weight) values ('title','title',1.0), ('body','body',0.8)",
            nativeQuery = true)
    void initData();

    @Query(value = "Select Count(*) from Field")
    Integer countAll();
}
