package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import engine.service.BeanAccess;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@Getter
public class ControlComponent {
    private static BeanAccess beanAccess;
    private final VerticalLayout mainLayout;

    public ControlComponent() {
        mainLayout = UIElement.getMainLayout();

        Button button = new Button("Закрыть приложение");
        button.setIcon(VaadinIcon.EXIT.create());

        button.addClickListener(event -> {
            ApplicationContext context = beanAccess.getContext();
            ((ConfigurableApplicationContext) context).close();
        });

        mainLayout.add(UIElement.getTopLayout("Завершение работы приложения", "xl", null));
        mainLayout.add(button);
    }

    public static void setDataAccess(BeanAccess beanAccess) {
        ControlComponent.beanAccess = beanAccess;
    }
}
