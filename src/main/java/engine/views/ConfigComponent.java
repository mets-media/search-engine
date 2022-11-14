package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import engine.entity.Config;
import engine.repository.ConfigRepository;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Getter
public class ConfigComponent {
    private VerticalLayout verticalLayout;
    private Grid grid;
    Button newOptionButton;
    Button editButton;
    Button deleteOptionButton;

    private static ConfigRepository configRepository;

    public ConfigComponent() {
        verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);

        grid = new Grid<>(Config.class, true);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);

        List<Grid.Column> columns = grid.getColumns();

        columns.get(0).setVisible(false);
        columns.get(1).setAutoWidth(true);
        columns.get(2).setHeader("Свойство");
        columns.get(3).setHeader("Значение");


        verticalLayout.add(createControlButtons());
        verticalLayout.add(grid);
    }

    private HorizontalLayout createControlButtons() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout horizontalLayoutForLabel = new HorizontalLayout();
        horizontalLayoutForLabel.setAlignItems(FlexComponent.Alignment.START);
        horizontalLayoutForLabel.setSizeUndefined();

        Label label = new Label("Таблица конфигурирования программы");
        label.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin", "0");

        //label.setWidthFull();


        horizontalLayoutForLabel.add(label);

        newOptionButton = new Button("Добавить");
        newOptionButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        //newOptionButton.setWidthFull();

        editButton = new Button("Редактировать");
        editButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        //editButton.setWidthFull();


        deleteOptionButton = new Button("Удалить");
        deleteOptionButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        //deleteOptionButton.setWidthFull();

        addListeners();

        horizontalLayout.add(horizontalLayoutForLabel, newOptionButton, editButton, deleteOptionButton);
        return horizontalLayout;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void addListeners() {
        newOptionButton.addClickListener(buttonClickEvent -> {
            showDialog(null);
        });

        editButton.addClickListener(buttonClickEvent -> {
            Optional<Config> config = grid.getSelectedItems().stream().findFirst();
            config.ifPresent(c -> {
                showDialog(c);
            });
        });

        deleteOptionButton.addClickListener(buttonClickEvent -> {
            Optional<Config> config = grid.getSelectedItems().stream().findFirst();
            config.ifPresent(c -> configRepository.delete(c));
            grid.setItems(configRepository.findAll());
        });
    }

    public static void setConfigRepository(ConfigRepository configRepository) {
        ConfigComponent.configRepository = configRepository;
    }

    private void showDialog(Config option) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);

        dialog.setHeaderTitle("Добавить новое свойство");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        TextField textFieldKey = new TextField("Key, Char(5)");
        textFieldKey.setMaxLength(5);

        TextField textFieldName = new TextField("Свойство:");
        TextField textFieldValue = new TextField("Значение:");
        horizontalLayout.add(textFieldKey, textFieldName, textFieldValue);
        dialog.add(horizontalLayout);

        if (!(option == null)) {
            dialog.setHeaderTitle("Редактировать свойство");
            textFieldKey.setValue(option.getKey());
            textFieldName.setValue(option.getName());
            textFieldValue.setValue(option.getValue());
        }

//        textFieldKey.addValueChangeListener(event -> {
//            String newValue = textFieldKey.getValue();
//            if (newValue.length() > 5)
//                showMessage("Размер Key 5 символов!", 1000, Notification.Position.MIDDLE);
//            textFieldKey.setValue(newValue.substring(0, 5));
//        });


        Button saveButton = new Button("Сохранить", e -> {
            if (textFieldName.isEmpty())
                showMessage("Введите имя Свойства", 1000, Notification.Position.MIDDLE);
            else {
                if (!(option == null)) {
                    option.setKey(textFieldKey.getValue());
                    option.setName(textFieldName.getValue());
                    option.setValue(textFieldValue.getValue());
                    try {
                        configRepository.save(option);
                    } catch (Exception exception) {
                        showMessage("Key - не уникален. Введите другой!", 2000, Notification.Position.MIDDLE);
                        return;
                    }
                } else
                    try {
                        configRepository.save(new Config(textFieldKey.getValue(),
                                textFieldName.getValue(),
                                textFieldValue.getValue()));
                    } catch (Exception exception) {
                        showMessage("Key - не уникален. Введите другой!", 2000, Notification.Position.MIDDLE);
                        return;
                    }


                grid.setItems(configRepository.findAll());
                dialog.close();
            }
        });
        Button cancelButton = new Button("Отменить", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    public static void showMessage(String text, int duration, Notification.Position position) {
        Notification notification = new Notification(text, duration);
        notification.setPosition(position);
        notification.open();
    }

}
