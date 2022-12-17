package engine.repository;

import engine.entity.Field;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FieldRepository extends JpaRepository<Field,Integer> {
    @Modifying
    @Transactional
    @Query(value = "Insert into Field (Id, Name, Selector, Weight, active) values " +
            "(-1, 'title','title',1.0, true), " +
            "(-2, 'body','body',0.8, true), " +
            "(-3, 'Заголовки h1, h2','h1, h2',0.9, false)",
            nativeQuery = true)
    void initData();

    @Query(value="Select Name from Field order by Name",nativeQuery = true)
    List<String> getAllNames(Pageable pageable);

    Field findByName(String cssName);

    List<Field> findByActive(Boolean active);
}
