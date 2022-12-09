package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

public class CreateUI {

    public static VerticalLayout getMainLayout() {
        var verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        return verticalLayout;
    }

    public static HorizontalLayout getTopLayout(String caption, String fontSize, List<Button> buttons) {
        var topLayout = new HorizontalLayout();
        topLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        topLayout.setAlignItems(FlexComponent.Alignment.END);

        var labelLayout = new HorizontalLayout();
        labelLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        labelLayout.setAlignItems(FlexComponent.Alignment.END);
        labelLayout.setSizeFull();


        Label label = new Label(caption);
        label.getStyle().set("font-size", "var(--lumo-font-size-" + fontSize + ")").set("margin", "0");
        labelLayout.add(label);

        var controlsLayout = new HorizontalLayout();
        controlsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        controlsLayout.setAlignItems(FlexComponent.Alignment.END);

        if (!(buttons == null))
            for (Button button : buttons)
                controlsLayout.add(button);

        topLayout.add(labelLayout, controlsLayout);
        return topLayout;
    }

    public static void hideAllVerticalLayouts(VerticalLayout parentLayout) {
        parentLayout.getChildren().forEach(component -> {
            if (component.getClass() == VerticalLayout.class)
                component.setVisible(false);
        });
    }

    public static Tabs createTabs(List<String> captions, Tabs.Orientation orientation) {
        Tabs tabs = new Tabs();
        tabs.setOrientation(orientation);
        tabs.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        for (String caption : captions) {
            Tab newTab = new Tab(caption);
            tabs.add(newTab);
        }
        return tabs;
    }

}
