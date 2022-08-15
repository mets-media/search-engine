package engine.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import engine.entity.Site;
import engine.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Route("manageSite")
public class ManageSite extends AppLayout implements HasUrlParameter<Long> {

    Long id;
    FormLayout formLayout;
    TextField urlText;
    Button saveButton;

    @Autowired
    SiteRepository siteRepository;

    public ManageSite() {
        //Создаем объекты для формы
        //formLayout = new FormLayout();
        formLayout = new FormLayout();
        urlText = new TextField("Сайт");
        saveButton = new Button("Сохранить");

        formLayout.setMaxWidth(300, Unit.PIXELS);

        formLayout.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "0");
        urlText.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "0");
        saveButton.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("margin", "0");

        //Добавим все элементы на форму
        //contactForm.add(firstName, secondName,fatherName,numberPhone,email,saveContact);
        formLayout.add(urlText, saveButton);
        setContent(formLayout);
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, Long siteId) {
        id = siteId;
        if (!id.equals(0)) {
            addToNavbar(new H3("Редактирование сайта"));
        } else {
            addToNavbar(new H3("Добавление сайта"));
        }
        fillForm();
    }

    public void fillForm() {
        if (!id.equals(0)) {
            Optional<Site> site = siteRepository.findById(id);
            site.ifPresent(x -> {
                urlText.setValue(x.getUrl());
            });
        }
        saveButton.addClickListener(clickEvent -> {
            //Создадим объект контакта получив значения с формы
            Site site = new Site();
            if (!id.equals(0)) {
                site.setId(id);
            }
            site.setUrl(urlText.getValue());
            siteRepository.save(site);

            Notification notification = new Notification(id.equals(0) ? "Сайт успешно добавлен" : "Изменения внесены", 1000);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.addDetachListener(detachEvent -> {
                UI.getCurrent().navigate(MainView.class);
            });
            formLayout.setEnabled(false);
            notification.open();
        });
    }
}