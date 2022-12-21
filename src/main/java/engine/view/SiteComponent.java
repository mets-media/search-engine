package engine.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.service.BeanAccess;
import engine.service.HtmlParsing;
import engine.service.Parser;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.START;
import static engine.view.CreateUI.showMessage;

@Getter
@Setter
public class SiteComponent {
    private static BeanAccess beanAccess;
    private final VerticalLayout mainLayout;
    private final Grid<Site> grid;
    public static void setDataAccess(BeanAccess beanAccess) {
        SiteComponent.beanAccess = beanAccess;
    }
    public SiteComponent() {

        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Сканирование сайтов", "xl", createButtons()));
        mainLayout.setMinHeight("100%");

        grid = new Grid<>(Site.class, false);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        CreateUI.setAllCheckboxVisibility(grid, true);

        //grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        grid.addColumn(Site::getName).setHeader("Наименование").setResizable(true).setSortable(true);
        grid.addColumn(Site::getUrl)
                .setHeader("Адрес(url)")
                .setResizable(true)
                .setSortable(true);
//        grid.addColumn(Site::getPageCount).setHeader("Страниц в базе").setResizable(true)
//                .setTextAlign(ColumnTextAlign.CENTER);

        grid.addColumn(Site::getStatus).setHeader("Статус").setResizable(true)
                .setTextAlign(ColumnTextAlign.CENTER);


        //grid.addColumn(new LocalDateTimeRenderer<>((ValueProvider<Site, LocalDateTime>) site ->
        //        site.getStatusTime())).setHeader("Дата статуса ").setResizable(true);

        //grid.addColumn(Site::getLastError).setHeader("Сообщение").setResizable(true);

        grid.setItemDetailsRenderer(createSiteDetailRenderer());

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        mainLayout.add(grid);


    }

    private List<Button> createButtons() {
        List<Button> buttons = new ArrayList<>();

        //========================= ТЕСТ ==========================================
        Button testButton = new Button("Тест");
        buttons.add(testButton);
        testButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        testButton.addClickListener(event -> {

        });
        //========================= ДОБАВИТЬ САЙТ ==========================================
        Button createButton = new Button("Добавить");
        buttons.add(createButton);
        createButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        createButton.addClickListener(buttonClickEvent -> {
            showNewSiteDialog();
        });

        //============================  Кнопка удаления Сайта  =================================
        Button deleteButton = new Button("Удалить");
        buttons.add(deleteButton);
        deleteButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        deleteButton.addClickListener(buttonClickEvent -> {
            List<Site> sitesForDelete = grid.getSelectedItems().stream().collect(Collectors.toList());
            if (sitesForDelete.isEmpty()) {
                showMessage("Не выбраны сайты для удаления", 1000, Notification.Position.MIDDLE);
                return;
            }
            showDeleteSiteDialog(sitesForDelete);
        });

        //========================= СКАНИРОВАТЬ САЙТ ==========================================
        Button parseButton = new Button("Сканировать");
        buttons.add(parseButton);
        parseButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        parseButton.addClickListener(buttonClickEvent -> {
            Parser.setDataAccess(grid, beanAccess);

            Set<Site> selectedSites = grid.getSelectedItems();
            selectedSites.forEach(site -> {
                grid.deselect(site); //после модификации - другой "site" - выделение не снимется
                Parser.getStopList().remove(site);

                site.setStatus(SiteStatus.DOWNLOADING);
                beanAccess.getSiteRepository().save(site);
                Parser.start(site);
            });
            grid.setItems(beanAccess.getSiteRepository().findAll());
        });

        //========================= СТОП СКАНИРОВАНИЕ ==========================================
        Button stopButton = new Button("Стоп!");
        buttons.add(stopButton);
        stopButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        stopButton.addClickListener(event -> {
            Set<Site> stopSites = grid.getSelectedItems();
            stopSites.forEach(site -> {
                Parser.stop(site);
                grid.deselect(site);
                site.setStatus(SiteStatus.STOPPED);
                beanAccess.getSiteRepository().save(site);
                site.setPageCount(beanAccess.getPageRepository().countBySiteId(site.getId()));
                beanAccess.getSiteRepository().save(site);

            });
            //grid.getDataProvider().refreshAll();
            grid.setItems(beanAccess.getSiteRepository().findAll());
        });
        return buttons;
    }


//    public static void setAllCheckboxVisibility(Grid<Site> grid, boolean visible) {
//        if (visible) {
//            ((GridMultiSelectionModel<?>) grid.getSelectionModel())
//                    .setSelectAllCheckboxVisibility(
//                            GridMultiSelectionModel.SelectAllCheckboxVisibility.VISIBLE
//                    );
//        } else
//            ((GridMultiSelectionModel<?>) grid.getSelectionModel())
//                    .setSelectAllCheckboxVisibility(
//                            GridMultiSelectionModel.SelectAllCheckboxVisibility.HIDDEN
//                    );
//    }

    private void showDeleteSiteDialog(List<Site> sites) {
        Dialog dialog = new Dialog();
        //dialog.setMaxHeight(300, Unit.PIXELS);
        dialog.setMaxHeight("30%");

        Button confirm = new Button("Удалить");
        confirm.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        Button cancel = new Button("Отмена");
        cancel.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        dialog.setHeaderTitle("Удалить выбранные сайты?");
        dialog.getFooter().add(cancel, confirm);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setDefaultHorizontalComponentAlignment(START);

        for (Site site : sites) {
            verticalLayout.add(new Label(site.getUrl()));
        }

        dialog.add(verticalLayout);

        confirm.addClickListener(clickEvent -> {

            sites.forEach(delSite -> {
                //new Thread(() -> pageRepository.deleteBySiteId(delSite.getId())).start();

                beanAccess.getSiteRepository().delete(delSite);

                try {
                    FileUtils.deleteDirectory(new File("data/" + HtmlParsing.getDomainName(delSite.getUrl())));
                } catch (IOException e) {
                    //throw new RuntimeException(e);
                }

            });

            dialog.close();
            Notification notification = new Notification("Удалени выполнено!", 1000);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.open();
            grid.setItems(beanAccess.getSiteRepository().findAll());
        });
        cancel.addClickListener(clickEvent -> {
            dialog.close();
        });
        dialog.open();
    }

    private void generateDialog(String title, Grid<Site> sourceGrid, int countFirstVisibleFields) {
        Dialog dialog = new Dialog();
        dialog.setMinWidth("30%");

        dialog.setHeaderTitle(title);

        Button confirmButton = new Button("Сохранить");
        Button cancelButton = new Button("Отменить", e -> dialog.close());
        dialog.getFooter().add(confirmButton, cancelButton);

        VerticalLayout verticalLayout = new VerticalLayout();

        List<Grid.Column<Site>> columns = sourceGrid.getColumns();

        List<TextField> fields = new ArrayList<>();
        List<String> titles = new ArrayList<>();

        int fieldsCount = 0;
        for (Grid.Column<Site> column : columns) {
            if (fieldsCount >= countFirstVisibleFields) {
                break;
            }
            String titleString = column.getElement().getChild(0).toString();
            titleString = titleString.substring(titleString.indexOf(">") + 1, titleString.lastIndexOf("<"));

            titles.add(titleString);
            TextField textField = new TextField(titleString);
            fields.add(textField);

            verticalLayout.add(textField);

            fieldsCount++;
        }


        dialog.add(verticalLayout);

        dialog.open();
    }

    private void showNewSiteDialog() {
        Dialog dialog = new Dialog();
        dialog.setModal(true);
        dialog.setMinWidth("30%");

        dialog.setHeaderTitle("Добавить сайт");

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        TextField textFieldName = new TextField("Наименование");
        textFieldName.setMinWidth("50%");
        TextField textFieldUrl = new TextField("url сайта [http://....]");
        //textFieldUrl.setValue("http://");
        textFieldUrl.setWidth("50%");
        horizontalLayout.add(textFieldName, textFieldUrl);
        dialog.add(horizontalLayout);

        Button saveButton = new Button("Сохранить", e -> {
            if (textFieldUrl.isEmpty())
                showMessage("URL сайта не может быть пустым!", 1000, Notification.Position.MIDDLE);
            else {
                String newUrl = textFieldUrl.getValue();

                if (newUrl.charAt(newUrl.length() - 1) == '/') newUrl = newUrl.substring(0, newUrl.length() - 1);

                Site site = new Site();
                site.setName(textFieldName.getValue());
                site.setUrl(newUrl);
                site.setStatus(SiteStatus.NEW_SITE);
                site.setStatusTime(LocalDateTime.now());
                site.setPageCount(0);
                beanAccess.getSiteRepository().save(site);

                grid.setItems(beanAccess.getSiteRepository().findAll());
                dialog.close();
            }
        });
        Button cancelButton = new Button("Отменить", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }

    private void updateSiteInfo() {

        grid.getSelectedItems().forEach(site -> {
            int pageCount = beanAccess.getPageRepository().countBySiteId(site.getId());
            site.setPageCount(pageCount);
            beanAccess.getSiteRepository().save(site);
            grid.setItems(beanAccess.getSiteRepository().findAll());
        });
    }


    private static class SiteDetailFormLayout extends FormLayout {
        private final TextField pageCountTextField = new TextField("Страниц в базе данных");
        private final TextField siteStatusTextField = new TextField("Статус");
        private final TextField statusTimeTextField = new TextField("время установки статуса");
        private final TextField lastErrorTextField = new TextField("Сообщение");

        public SiteDetailFormLayout() {
            Stream.of(pageCountTextField, siteStatusTextField, statusTimeTextField, lastErrorTextField)
                    .forEach(field -> {
                        field.setReadOnly(true);
                        field.setSizeFull();
                    });

            var horizontalLayout = new HorizontalLayout(pageCountTextField, siteStatusTextField, statusTimeTextField);
            horizontalLayout.setAlignItems(START);
            horizontalLayout.setSizeFull();

            var verticalLayout = new VerticalLayout(horizontalLayout, lastErrorTextField);
            verticalLayout.setAlignItems(START);

            add(verticalLayout);
        }

        public void setSite(Site site) {
            Integer pageCount = beanAccess.getPageRepository().countBySiteId(site.getId());
            site.setPageCount(pageCount);
            pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));
            siteStatusTextField.setValue(site.getStatus().toString());

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy (HH:mm)");
            statusTimeTextField.setValue(dateTimeFormatter.format(site.getStatusTime()));

            String lastError = site.getLastError();
            if (!(lastError == null))
                lastErrorTextField.setValue(lastError);
        }
    }

    private static ComponentRenderer<SiteDetailFormLayout, Site> createSiteDetailRenderer() {
        return new ComponentRenderer<>(SiteDetailFormLayout::new,
                SiteDetailFormLayout::setSite);
    }


}
