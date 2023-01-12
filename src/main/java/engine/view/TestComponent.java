package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.service.BeanAccess;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TestComponent {
    private static BeanAccess beanAccess;
    private final VerticalLayout mainLayout;

    public TestComponent() {
        mainLayout = CreateUI.getMainLayout();

        var statGrid = createSiteGrid();

        Button button = new Button();
        button.setIcon(VaadinIcon.CALC.create());
        button.addClickListener(event -> {
            statGrid.setItems(beanAccess.getSiteRepository().getStatistic());
        });

        List<Button> listButton = new ArrayList<>();
        listButton.add(button);

        mainLayout.add(CreateUI.getTopLayout("Информация о сайтах", "xl", listButton));
        mainLayout.add(statGrid);
    }

    private Grid createSiteGrid() {
        Grid<Site> grid = new Grid<>(Site.class,false);
        grid.addColumn("name").setHeader("Наименование").setResizable(true).setSortable(true);
        grid.addColumn("url")
                .setHeader("Адрес(url)")
                .setResizable(true)
                .setSortable(true);
        grid.addColumn("pageCount").setHeader("Стрю");
        grid.addColumn("lemmaCount").setHeader("Леммы");
        grid.addColumn("indexCount").setHeader("Индексы");
        grid.addColumn("status").setHeader("Статус");

        grid.addComponentColumn(item -> {
            ProgressBar progressBar = new ProgressBar();
            progressBar.setIndeterminate(false);
            progressBar.setMin(0l);
            progressBar.setMax(100l);
            progressBar.setValue(0l);
            progressBar.setVisible(true);

            progressBar.addAttachListener(attachEvent -> {
                switch ((SiteStatus) item.getStatus()) {
                    case INDEXING -> {
                        progressBar.setIndeterminate(true);
                    }
                    case INDEXED -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(100L);
                    }
                    case STOPPED -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(10L);
                    }
                    default -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(0l);
                    }
                }
            });
            return progressBar;
        });

        return grid;
    }

    public static void setDataAccess(BeanAccess beanAccess) {
        TestComponent.beanAccess = beanAccess;
    }
}
