package engine.grid;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import engine.auxEntity.FieldType;
import engine.auxEntity.MainGrid;
import engine.entity.Site;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


@Getter
public class GridBufferedInlineEditor {

    private JpaRepository<MainGrid, Long> repository;
    private Grid grid;

    public GridBufferedInlineEditor(JpaRepository<MainGrid, Long> repository) {
        this.repository = repository;
        this.grid = new Grid<>(Site.class, false);
    }

    public void addColumns(List<MainGrid> columnsInfo) {
        Editor editor = grid.getEditor();

        Binder<Site> binder = new Binder<>(Site.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        columnsInfo.forEach(column -> {
            System.out.println("fieldName " + column.getFieldName());
            FieldType fType = (FieldType) column.getFieldType();
            switch (fType) {
                case TEXT_FIELD -> {
                    System.out.println("Текстовое поле " + fType);
                    Grid.Column<Site> textColumn = grid.addColumn(column.getFieldName())
                            .setHeader(column.getCaption());

                    TextField textField = new TextField();
                    if (column.getWidth().equals(0)) {textField.setWidthFull();}
                    else {textField.setWidth(column.getWidth() + "px");}


                    binder.forField(textField)
                            .asRequired("url сайта не может быть пустым!")
                            .withStatusLabel(new Label("ValidationMessage"))
                            .bind(Site::getUrl, Site::setUrl);

                    textColumn.setEditorComponent(textField);

                }
                case EDIT_BUTTON -> {
                    System.out.println("Создание колонки " + fType);
                    Grid.Column<Site> editColumn =
                            grid.addComponentColumn(site -> {
                                Button editButton = new Button("Edit");
                                editButton.getStyle()
                                        .set("font-size", "var(--lumo-font-size-l)")
                                        .set("margin", "0");

                                editButton.addClickListener(e -> {
                                    if (editor.isOpen())
                                        editor.cancel();
                                    grid.getEditor().editItem(site);
                                });
                                return editButton;
                            //}).setAutoWidth(true).setFlexGrow(0);
                                //}).setWidth(150 + "px").setFlexGrow(0);
                    }).setWidth(column.getWidth() + "px").setFlexGrow(0);


                    Button saveButton = new Button("Save", e -> editor.save());

                    Button cancelButton = new Button(VaadinIcon.CLOSE.create(),
                            e -> editor.cancel());

                    cancelButton.addThemeVariants(ButtonVariant.LUMO_ICON,
                            ButtonVariant.LUMO_ERROR);

                    HorizontalLayout actions = new HorizontalLayout(saveButton,
                            cancelButton);

                    actions.setPadding(false);

                    editColumn.setEditorComponent(actions);

                }


            }


        });
    }


    protected void createGrid() {

        Grid<Site> grid = new Grid<>(Site.class, false);
        Editor<Site> editor = grid.getEditor();
        grid.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "0");

        List<MainGrid> columns = repository.findAll();

        addColumns(columns);

        Binder<Site> binder = new Binder<>(Site.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

//        TextField firstNameField = new TextField();
//        firstNameField.setWidthFull();
//        binder.forField(firstNameField).
//                asRequired("First name must not be empty")
//                .withStatusLabel(firstNameValidationMessage)
//                .bind(Site::getFirstName, Site::setFirstName);
//
//        firstNameColumn.setEditorComponent(firstNameField);
//
//        TextField lastNameField = new TextField();
//        lastNameField.setWidthFull();
//        binder.forField(lastNameField).
//
//                asRequired("Last name must not be empty")
//                .withStatusLabel(lastNameValidationMessage)
//                .bind(Site::getLastName, Site::setLastName);
//        lastNameColumn.setEditorComponent(lastNameField);
//
//        EmailField emailField = new EmailField();
//        emailField.setWidthFull();
//        binder.forField(emailField).
//                asRequired("Email must not be empty")
//                .withValidator(new EmailValidator("Please enter a valid email address"))
//                .withStatusLabel(emailValidationMessage)
//                .bind(Site::getEmail, Site::setEmail);
//
//        emailColumn.setEditorComponent(emailField);
//
//        Button saveButton = new Button("Save", e -> editor.save());
//        Button cancelButton = new Button(VaadinIcon.CLOSE.create(),
//                e -> editor.cancel());
//        cancelButton.addThemeVariants(ButtonVariant.LUMO_ICON,
//                ButtonVariant.LUMO_ERROR);
//        HorizontalLayout actions = new HorizontalLayout(saveButton,
//                cancelButton);
//        actions.setPadding(false);
//        editColumn.setEditorComponent(actions);
    }

}
