package engine.views;


import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import engine.entity.Site;
import engine.repository.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.List;

@Route("sites")
public class SiteList extends AppLayout {
    VerticalLayout layout;
    Grid<Site> grid;
    RouterLink linkCreate;
    RouterLink linkToStart;

    @Autowired
    SiteRepository siteRepository;

    public SiteList(){
        layout = new VerticalLayout();
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);

        grid = new Grid<>();

        linkCreate = new RouterLink("Добавить сайт", ManageSite.class,0l);
        layout.add(linkCreate);

        linkToStart = new RouterLink("Перейти в начало", MainView.class);
        layout.add(linkToStart);

        layout.add(grid);

        addToNavbar(new H3("Список сайтов"));

        //addToDrawer(new H1("Сайты"));
        setContent(layout);
    }

    @PostConstruct
    public void fillGrid(){
        List<Site> sites = siteRepository.findAll();
        if (!sites.isEmpty()){
            //Выведем столбцы в нужном порядке
            grid.addColumn(Site::getUrl).setHeader("Сайт");
            //Добавим кнопку удаления и редактирования
            grid.addColumn(new NativeButtonRenderer<Site>("Редактировать", site -> {
                UI.getCurrent().navigate(ManageSite.class,site.getId());
            }));
            grid.addColumn(new NativeButtonRenderer<Site>("Удалить", site -> {
                Dialog dialog = new Dialog();
                Button confirm = new Button("Удалить");
                Button cancel = new Button("Отмена");
                dialog.add("Вы уверены что хотите удалить сайт?");
                dialog.add(confirm);
                dialog.add(cancel);
                confirm.addClickListener(clickEvent -> {
                    siteRepository.delete(site);
                    dialog.close();
                    Notification notification = new Notification("Сайт удален",1000);
                    notification.setPosition(Notification.Position.MIDDLE);
                    notification.open();
                    grid.setItems(siteRepository.findAll());
                });
                cancel.addClickListener(clickEvent -> {
                    dialog.close();
                });
                dialog.open();
            }));
            grid.setItems(sites);
        }
    }
}