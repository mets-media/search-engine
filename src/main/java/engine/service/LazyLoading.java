package engine.service;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.EntityManager;

public class LazyLoading {


    public static void gridSetItemsThrowEntityManager(Grid grid, EntityManager entityManager) {

    }

    public static void gridSetItemsSortedBy(Grid grid, JpaRepository repository) {
//            grid.addColumn(Page::getPath)
//                    .setHeader("Path")
//                    .setKey("path")  //внимание! Ключь, который передаётся в callback
//                    .setSortable(true)
//                    .setResizable(true);
        grid.setItems(VaadinSpringDataHelpers.fromPagingRepository(repository));
    }
    public static void gridSetItemsFindAll(Grid grid, JpaRepository repository) {
        grid.setItems(query -> {
            return repository.findAll(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });
    }
}
