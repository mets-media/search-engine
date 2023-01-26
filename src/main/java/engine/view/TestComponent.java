package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.service.BeanAccess;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TestComponent {
    private static BeanAccess beanAccess;
    private final VerticalLayout mainLayout;

    public TestComponent() {
        mainLayout = UIElement.getMainLayout();

        Button button = new Button("Закрыть приложение");
        button.setIcon(VaadinIcon.EXIT.create());

        button.addClickListener(event -> {
            ApplicationContext context = beanAccess.getContext();
            ((ConfigurableApplicationContext) context).close();
        });

        //List<Button> listButton = new ArrayList<>();
        //listButton.add(button);

        mainLayout.add(UIElement.getTopLayout("Завершение приложения", "xl", null));
        mainLayout.add(button);

    }



    public static void setDataAccess(BeanAccess beanAccess) {
        TestComponent.beanAccess = beanAccess;
    }
}
