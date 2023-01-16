package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static engine.view.UIElement.showMessage;

@Getter
public class ConfigComponent {
    private VerticalLayout mainLayout;
    private Grid<Config> grid;
    Button newOptionButton;
    Button editButton;
    Button deleteOptionButton;

    private static ConfigRepository configRepository;

    public ConfigComponent() {
        mainLayout = UIElement.getMainLayout();
        List<Button> buttons = createButtons(List.of("Добавить", "Редактировать", "Удалить"));
        mainLayout.add(UIElement.getTopLayout("Настройки сканирования", "xl", buttons));

        grid = new Grid(Config.class, false);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);

        grid.addColumn(Config::getKey).setHeader("Key").setAutoWidth(true)
                .setSortable(true)
                .setTextAlign(ColumnTextAlign.CENTER)
                        .setVisible(false);
        grid.addColumn(Config::getName).setHeader("Наименование").setSortable(true);
        grid.addColumn(Config::getValue).setHeader("Значение");

        mainLayout.add(grid);
    }

    private HorizontalLayout createControlButtons() {
        var topLayout = new HorizontalLayout();
        topLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        topLayout.setAlignItems(FlexComponent.Alignment.END);

        //=================       Название       ====================
        var labelLayout = new HorizontalLayout();
        labelLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        labelLayout.setAlignItems(FlexComponent.Alignment.START);
        labelLayout.setSizeFull();

        Label label = new Label("Таблица конфигурирования программы");
        label.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("margin", "0");

        labelLayout.add(label);

        newOptionButton = new Button("Добавить");
        newOptionButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        editButton = new Button("Редактировать");
        editButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        deleteOptionButton = new Button("Удалить");
        deleteOptionButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        addListeners();

        var controlsLayout = new HorizontalLayout(newOptionButton, editButton, deleteOptionButton);
        controlsLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        controlsLayout.setAlignItems(FlexComponent.Alignment.END);
        controlsLayout.setSizeUndefined();

        topLayout.add(labelLayout, controlsLayout);
        return topLayout;
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

    private List<Button> createButtons(List<String> captions) {
        List<Button> buttons = new ArrayList<>();
        for (String caption : captions) {

            var button = new Button(caption);
            button.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
            buttons.add(button);
            switch (button.getText()) {
                case "Добавить" -> {
                    button.addClickListener(buttonClickEvent -> {
                        showDialog(null);
                    });
                }
                case "Редактировать" -> {
                    button.addClickListener(buttonClickEvent -> {
                        Optional<Config> config = grid.getSelectedItems().stream().findFirst();
                        config.ifPresent(c -> {
                            showDialog(c);
                        });
                    });
                }
                case "Удалить" -> {
                    button.addClickListener(buttonClickEvent -> {
                        Optional<Config> config = grid.getSelectedItems().stream().findFirst();
                        config.ifPresent(c -> {
                            if (c.getId() < 0)
                                showMessage(c.getName() + "нельзя удалять!");
                            else
                                configRepository.delete(c);
                        });
                        grid.setItems(configRepository.findAll());
                    });
                }
            }
        }

        return buttons;
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
                showMessage("Введите имя Свойства");
            else {
                if (!(option == null)) {
                    option.setKey(textFieldKey.getValue());
                    option.setName(textFieldName.getValue());
                    option.setValue(textFieldValue.getValue());
                    try {
                        configRepository.save(option);
                    } catch (Exception exception) {
                        showMessage("Key - не уникален. Введите другой!");
                        return;
                    }
                } else
                    try {
                        configRepository.save(new Config(textFieldKey.getValue(),
                                textFieldName.getValue(),
                                textFieldValue.getValue()));
                    } catch (Exception exception) {
                        showMessage("Key - не уникален. Введите другой!");
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


}
