package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import engine.entity.Config;
import engine.repository.ConfigRepository;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

/*
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
*/

    private List<Button> createButtons(List<String> captions) {
        List<Button> buttons = new ArrayList<>();
        for (String caption : captions) {

            var button = new Button(caption);
            button.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
            buttons.add(button);
            switch (button.getText()) {
                case "Добавить" -> {
                    button.setEnabled(false);
                    button.addClickListener(buttonClickEvent -> {
                        showDialog(null);
                    });
                }
                case "Редактировать" -> {
                    button.addClickListener(buttonClickEvent -> {
                        Optional<Config> config = grid.getSelectedItems().stream().findFirst();
                        config.ifPresent(c -> {
                            //showDialog(c);
                            getDialog(c);
                        });
                    });
                }
                case "Удалить" -> {
                    button.setEnabled(false);
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

    private void getDialog(Config option) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);

        dialog.setHeaderTitle(option.getName());
        switch (option.getValueType()) {
            case INTEGER -> {
                IntegerField integerField = new IntegerField();
                integerField.setId("Integer");
                integerField.setMax(5000);
                integerField.setMin(0);
                integerField.setAutoselect(true);
                integerField.setValue(Integer.parseInt(option.getValue()));
                dialog.add(integerField);
            }
            case BOOLEAN -> {
                ComboBox<Boolean> comboBox = new ComboBox<>();
                comboBox.setId("Boolean");
                comboBox.setItems(List.of(true, false));
                comboBox.setValue(Boolean.parseBoolean(option.getValue()));
                dialog.add(comboBox);
            }
            case POSITION -> {
                ComboBox<Notification.Position> comboBox = new ComboBox<>();
                comboBox.setId("Position");
                comboBox.setItemLabelGenerator(Enum::name);
                comboBox.setItems(List.of(Notification.Position.MIDDLE, Notification.Position.BOTTOM_END));
                switch (option.getValue()) {
                    case "MIDDLE" -> comboBox.setValue(Notification.Position.MIDDLE);
                    case "BOTTOM_END" -> comboBox.setValue(Notification.Position.BOTTOM_END);
                }
                dialog.add(comboBox);
            }
        }

        Button saveButton = new Button("Сохранить", event -> {
            AtomicReference<String> result = new AtomicReference<>("");
            dialog.getChildren().forEach(component -> {

                System.out.println(component.getClass().getName());
                System.out.println(component.getId());

                String id = String.valueOf(component.getId());
                switch (id) {
                    case "Optional[Integer]" ->
                            result.set(Integer.toString(Math.round(((IntegerField) component).getValue())));
                    case "Optional[Boolean]", "Optional[Position]" ->
                            result.set(((ComboBox<?>) component).getValue().toString());
                }
                option.setValue(result.get());
                configRepository.save(option);

                grid.setItems(configRepository.findAll());
                dialog.close();
            });
        });
        Button cancelButton = new Button("Отменить", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void showDialog(Config option) {
        Dialog dialog = new Dialog();
        dialog.setModal(true);

        dialog.setHeaderTitle("Добавить новое свойство");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        TextField textFieldKey = new TextField("Key, Char(5)");
        textFieldKey.setMaxLength(5);
        textFieldKey.setVisible(false);

        TextField textFieldName = new TextField("Свойство:");
        textFieldName.setVisible(false);
        TextField textFieldValue = new TextField("Значение:");
        horizontalLayout.add(textFieldKey, textFieldName, textFieldValue);

        ComboBox<String> comboBox = new ComboBox<>("Тип переменной");
        comboBox.setItems(List.of("Integer", "Boolean", "Position"));

        comboBox.addValueChangeListener(event -> {
            switch (event.getValue()) {
                case "Integer" -> option.setValueType(Config.ValueType.INTEGER);
                case "Boolean" -> option.setValueType(Config.ValueType.BOOLEAN);
                case "Position" -> option.setValueType(Config.ValueType.POSITION);
            }
            configRepository.save(option);
        });

        dialog.add(horizontalLayout, comboBox);


        if (!(option == null)) {
            dialog.setHeaderTitle(option.getName());
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
