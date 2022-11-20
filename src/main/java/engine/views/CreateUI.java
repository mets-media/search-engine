package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

public class CreateUI {

    public static VerticalLayout getMainLayout() {
        var verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        return verticalLayout;
    }
    public static HorizontalLayout getTopLayout(String caption, List<Button> buttons) {
        var topLayout = new HorizontalLayout();
        topLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        topLayout.setAlignItems(FlexComponent.Alignment.END);

        var labelLayout = new HorizontalLayout();
        labelLayout.setAlignItems(FlexComponent.Alignment.END);
        labelLayout.setSizeFull();

        Label label = new Label(caption);
        label.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin", "0");
        labelLayout.add(label);

        var controlsLayout = new HorizontalLayout();
        controlsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        controlsLayout.setAlignItems(FlexComponent.Alignment.END);

        for (Button button : buttons)
            controlsLayout.add(button);

        topLayout.add(labelLayout, controlsLayout);
        return topLayout;
    }
}
