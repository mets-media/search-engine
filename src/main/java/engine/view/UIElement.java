package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import engine.service.TimeMeasure;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class UIElement {
    private static int showMessageTime = 2000;

    private static Notification.Position position = Notification.Position.MIDDLE;

    public static void setShowMessageTime(int showMessageTime) {
        UIElement.showMessageTime = showMessageTime;
    }

    public static void setPosition(Notification.Position position) {
        UIElement.position = position;
    }

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

    public static void setAllCheckboxVisibility(Grid grid, boolean visible) {
        if (visible) {
            ((GridMultiSelectionModel<?>) grid.getSelectionModel())
                    .setSelectAllCheckboxVisibility(
                            GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE
                    );
        } else
            ((GridMultiSelectionModel<?>) grid.getSelectionModel())
                    .setSelectAllCheckboxVisibility(
                            GridMultiSelectionModel.SelectAllCheckboxVisibility.HIDDEN
                    );
    }

    public static void showMessage(String text) {
        Notification notification = new Notification(text, showMessageTime, position);
        notification.setPosition(position);
        notification.open();
    }

    public static void showMessageWith(String text, int duration, Notification.Position position) {
        Notification notification = new Notification(text, duration);
        notification.setPosition(position);

        notification.addDetachListener(detachEvent -> {

        });

        notification.open();
    }

    public static Grid<String> getStringGrid(String caption, List<String> words) {
        Grid<String> grid = new Grid<>(String.class, false);
        Grid.Column<String> col1 = grid.addColumn(String::toString)
                .setHeader(caption)
                .setResizable(true)
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.START)
                .setFooter(createWordsCountFooterText(words));

        grid.setItems(words);
        return grid;
    }
    private Grid<String> getStringGridWithHeader(String caption, String colName, List<String> words) {
        Grid<String> grid = new Grid<>(String.class, false);
        Grid.Column<String> col1 = grid.addColumn(String::toString).setHeader(colName).setTextAlign(ColumnTextAlign.START);
        Grid.Column<String> col2 = grid.addColumn(String::toString).setHeader(colName + "_");
        col2.setVisible(false);
        grid.setItems(words);

        HeaderRow headerRow = grid.prependHeaderRow();

        Div simpleCell = new Div();
        simpleCell.setText(caption);
        simpleCell.getElement().getStyle().set("text-align", "center");
        headerRow.join(col1, col2).setComponent(simpleCell);
        return grid;
    }
    private static String createWordsCountFooterText(List<String> words) {
        int count = words.size();
        if (count > 1)
            return "Всего: " + count;
        else
            return "";
    }

    public static void removeComponentById(VerticalLayout container, String deleteId) {

        container.getChildren().forEach(component -> {
            component.getId().ifPresent(id -> {
                if (id.equals(deleteId)) {
                    container.remove(component);
                }
            });
        });
    }


    public static Button createButton(String text, VaadinIcon icon, String title) {
        Button button = new Button(text);
        if (!(icon == null)) button.setIcon(icon.create());
        button.getElement().setProperty("title", title);

        return button;
    }

    public static ComboBox<String> createComboBox(List<String> items) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setItems(items);
        comboBox.setValue(items.get(0));
        return comboBox;
    }


}
