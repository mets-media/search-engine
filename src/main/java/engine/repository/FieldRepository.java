package engine.repository;

import engine.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

public interface FieldRepository extends JpaRepository<Field,Integer> {
    @Modifying
    @Transactional
    @Query(value = "Insert into Field (Id, Name, Selector, Weight) values (-1, 'title','title',1.0), (-2, 'body','body',0.8)",
            nativeQuery = true)
    void initData();

    @Query(value = "Select Count(*) from Field")
    Integer countAll();
}
