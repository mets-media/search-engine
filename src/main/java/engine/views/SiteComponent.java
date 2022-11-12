package engine.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridMultiSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.repository.PageRepository;
import engine.repository.SiteRepository;
import engine.service.Parser;
import lombok.Getter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Getter
public class SiteComponent {

    private VerticalLayout verticalLayout;
    private Grid<Site> grid;
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static JdbcTemplate jdbcTemplate;


    public SiteComponent() {
        verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.END);

        grid = new Grid<>(Site.class, false);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        setAllCheckboxVisibility(grid, true);

        grid.addColumn(Site::getUrl).setHeader("Сайт");
        grid.addColumn(Site::getPageCount).setHeader("Страниц в базе");
        grid.addColumn(Site::getStatus).setHeader("Статус");

        //Создание кнопок управления
        HorizontalLayout hLayout = createButtons();

        verticalLayout.add(hLayout);
        verticalLayout.add(grid);
    }

    private HorizontalLayout createButtons() {
        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);


        //========================= ТЕСТ ==========================================
        Button testButton = new Button("Тест");
        testButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        testButton.addClickListener(event -> {
            //updateSiteInfo();
        });
        //========================= ДОБАВИТЬ САЙТ ==========================================
        Button createButton = new Button("Добавить");
        createButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        createButton.addClickListener(buttonClickEvent -> {
            showNewSiteDialog();
        });

        //============================  Кнопка удаления Сайта  =================================
        Button deleteButton = new Button("Удалить");
        deleteButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        deleteButton.addClickListener(buttonClickEvent -> {
            List<Site> sitesForDelete = grid.getSelectedItems().stream().collect(Collectors.toList());
            if (sitesForDelete.isEmpty()) {
                ConfigComponent.showMessage("Не выбраны сайты для удаления", 1000, Notification.Position.MIDDLE);
                return;
            }
            showDeleteSiteDialog(sitesForDelete);
        });


        //========================= СКАНИРОВАТЬ САЙТ ==========================================
        Button parseButton = new Button("Сканировать");
        parseButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        parseButton.addClickListener(buttonClickEvent -> {
            Parser.setPageRepository(pageRepository);
            Parser.setSiteRepository(siteRepository);
            Parser.setJdbcTemplate(jdbcTemplate);

            Set<Site> selectedSites = grid.getSelectedItems();
            selectedSites.forEach(site -> {
                grid.deselect(site); //после модификации - другой "site" - выделение не снимется
                Parser.getStopList().remove(site);

                site.setStatus(SiteStatus.DOWNLOADING);
                siteRepository.save(site);
                Parser.start(site);
            });
            grid.setItems(siteRepository.findAll());

            //setContent(getSimpleGrid());
        });

        //========================= СТОП СКАНИРОВАНИЕ ==========================================
        Button stopButton = new Button("Стоп!");
        stopButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        stopButton.addClickListener(event -> {
            Set<Site> stopSites = grid.getSelectedItems();
            stopSites.forEach(site -> {
                //parser.stopScanSite(site);
                Parser.stop(site); //Новый вариант
                grid.deselect(site);
                site.setStatus(SiteStatus.STOPPED);
                siteRepository.save(site);
            });
            //grid.getDataProvider().refreshAll();
            grid.setItems(siteRepository.findAll());
        });

        hLayout.add(testButton, createButton, deleteButton, parseButton, stopButton);
        return hLayout;
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

    private void showDeleteSiteDialog(List<Site> sites) {
        Dialog dialog = new Dialog();
        Button confirm = new Button("Удалить");
        Button cancel = new Button("Отмена");

        dialog.add("Удалить выбранные сайты?");
        dialog.add(confirm);
        dialog.add(cancel);
        confirm.addClickListener(clickEvent -> {

            //Optional<Site> site = grid.getSelectedItems().stream().findFirst();
            //siteRepository.delete(site.get());

            sites.forEach(delSite -> siteRepository.delete(delSite));

            dialog.close();
            Notification notification = new Notification("Удалени выполнено!", 1000);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.open();
            grid.setItems(siteRepository.findAll());
        });
        cancel.addClickListener(clickEvent -> {
            dialog.close();
        });
        dialog.open();
    }

    private void showNewSiteDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);

        dialog.setHeaderTitle("Добавить сайт");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        TextField textFieldUrl = new TextField("URL:");
        horizontalLayout.add(textFieldUrl);
        dialog.add(horizontalLayout);

        Button saveButton = new Button("Сохранить", e -> {
            if (textFieldUrl.isEmpty())
                ConfigComponent.showMessage("URL сайта не может быть пустым!", 1000, Notification.Position.MIDDLE);
            else {
                String newUrl = textFieldUrl.getValue();

                if (newUrl.charAt(newUrl.length() - 1) == '/') newUrl = newUrl.substring(0, newUrl.length() - 1);

                Site site = new Site();
                site.setUrl(newUrl);
                siteRepository.save(site);

                grid.setItems(siteRepository.findAll());
                dialog.close();
            }
        });
        Button cancelButton = new Button("Отменить", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    public static void setDataAccess(SiteRepository siteRepository, PageRepository pageRepository, JdbcTemplate jdbcTemplate) {
        SiteComponent.siteRepository = siteRepository;
        SiteComponent.pageRepository = pageRepository;
        SiteComponent.jdbcTemplate = jdbcTemplate;

    }

}
