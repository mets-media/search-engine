package engine.view;

import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import engine.entity.Site;
import engine.entity.SiteStatus;
import engine.repository.SiteRepository;
import engine.service.BeanAccess;
import engine.service.Parser;
import engine.service.TimeMeasure;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END;
import static com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.START;
import static engine.view.UIElement.showMessage;

@Component
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
        mainLayout = UIElement.getMainLayout();
        mainLayout.add(UIElement.getTopLayout("Сканирование сайтов", "xl", createButtons()));
        mainLayout.setMinHeight("100%");

        grid = new Grid<>(Site.class, false);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        UIElement.setAllCheckboxVisibility(grid, true);

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

        grid.addComponentColumn(item -> {
            ProgressBar progressBar = new ProgressBar();
            progressBar.setIndeterminate(false);
            progressBar.setMin(0l);
            progressBar.setMax(100l);
            progressBar.setValue(0l);
            progressBar.setVisible(true);

            progressBar.addAttachListener(attachEvent -> {
                switch ((SiteStatus) item.getStatus()) {
                    case INDEXING -> {
                        progressBar.setIndeterminate(true);
                    }
                    case INDEXED -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(100L);
                    }
                    case STOPPED -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(10L);
                    }
                    default -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setValue(0l);
                    }
                }
            });
            return progressBar;
        });

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
//            grid.getSelectedItems().forEach(site -> {
//                site.setStatus(SiteStatus.INDEXED);
//                beanAccess.getSiteRepository().save(site);
//            });

            SiteRepository siteRepository = beanAccess.getSiteRepository();
            for (Site site : siteRepository.getStatistic()) {
                siteRepository.save(site);
            }


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
                showMessage("Не выбраны сайты для удаления");
                return;
            }
            showDeleteSiteDialog(sitesForDelete);
        });

        //========================= СКАНИРОВАТЬ САЙТ ==========================================
        Button parseButton = new Button("Сканировать");
        buttons.add(parseButton);
        parseButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");

        parseButton.addClickListener(buttonClickEvent -> {


            Parser.setDataAccess(beanAccess);

            Set<Site> selectedSites = grid.getSelectedItems();
            selectedSites.forEach(site -> {
                grid.deselect(site); //после модификации - другой "site" - выделение не снимется

                SiteStatus status = (SiteStatus) site.getStatus();
                if ((status.equals(SiteStatus.NEW_SITE)) || (status.equals(SiteStatus.STOPPED))) {
                    Parser.getStopList().remove(site);

                    site.setStatus(SiteStatus.INDEXING);
                    beanAccess.getSiteRepository().save(site);
                    //Проверка для сброса счётчиков удалений если max(page.id) = 0
                    beanAccess.getConfigRepository().resetSequences();
                    Parser.start(site);
                }
            });
            grid.setItems(beanAccess.getSiteRepository().findAll());
            beanAccess.setUi(UI.getCurrent());
        });

        //========================= СТОП СКАНИРОВАНИЕ ==========================================
        Button stopButton = new Button("Стоп!");
        buttons.add(stopButton);
        stopButton.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("margin", "0");
        stopButton.addClickListener(event -> {
            Set<Site> stopSites = grid.getSelectedItems();
            stopSites.forEach(site -> {
                grid.deselect(site);
                if (site.getStatus().equals(SiteStatus.INDEXING)) {
                    Parser.stop(site);
                    site.setStatus(SiteStatus.STOPPED);
                    site.setStatusTime(LocalDateTime.now());
                    beanAccess.getSiteRepository().save(site);
                    site.setPageCount(beanAccess.getPageRepository().countBySiteId(site.getId()));
                    beanAccess.getSiteRepository().save(site);
                }
            });
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
                SiteStatus status = (SiteStatus) delSite.getStatus();
                switch (status) {
                    case NEW_SITE, STOPPED, INDEXED, FAILED -> beanAccess.getSiteRepository().delete(delSite);
                    default -> grid.deselect(delSite);
                }

            });
            dialog.close();
            Notification notification = new Notification("Удаление выполнено!", 1000);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.addDetachListener(detachEvent -> {
               grid.setItems(beanAccess.getSiteRepository().findAll());
            });
            notification.open();
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
                showMessage("URL сайта не может быть пустым!");
            else {
                String newUrl = textFieldUrl.getValue();

                if (newUrl.charAt(newUrl.length() - 1) == '/') newUrl = newUrl.substring(0, newUrl.length() - 1);

                Site site = new Site();
                site.setName(textFieldName.getValue());
                site.setUrl(newUrl);
                site.setStatus(SiteStatus.NEW_SITE);
                site.setStatusTime(LocalDateTime.now());
                site.setPageCount(0); site.setIndexCount(0); site.setLemmaCount(0);
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
        private final TextField pageCountTextField = new TextField("Страниц в базе");
        private final TextField siteStatusTextField = new TextField("Статус");
        private final TextField statusTimeTextField = new TextField("Время статуса");
        private final TextField lastErrorTextField = new TextField("Сообщение");

        public SiteDetailFormLayout() {
            var verticalLayout = new VerticalLayout();
            verticalLayout.setWidthFull();
            verticalLayout.setAlignItems(END);

            var horizontalLayout = new HorizontalLayout(pageCountTextField,
                    siteStatusTextField,
                    statusTimeTextField,
                    lastErrorTextField);


            verticalLayout.add(horizontalLayout);
            horizontalLayout.setAlignItems(END);
            horizontalLayout.setWidth("100%");

            Stream.of(pageCountTextField, siteStatusTextField, statusTimeTextField)
                    .forEach(field -> field.setReadOnly(true));

            pageCountTextField.setWidth("20%");
            siteStatusTextField.setWidth("30%");
            statusTimeTextField.setWidth("40%");

            lastErrorTextField.setWidthFull();



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
