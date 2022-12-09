package engine.views;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import engine.entity.Lemma;
import engine.repository.*;
import engine.service.Lemmatization;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import javax.persistence.EntityManager;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchComponent {
    private static SiteRepository siteRepository;
    private static PageRepository pageRepository;
    private static LemmaRepository lemmaRepository;
    private static PartOfSpeechRepository partOfSpeechRepository;
    private final VerticalLayout mainLayout;

    private Grid<Lemma> lemmaGrid = new Grid<>(Lemma.class, false);
    private final TextField pageCountTextField = new TextField("Количество страниц");
    private final TextField lemmaCountTextField = new TextField("Количество лемм");
    private final TextField requestTextField = new TextField("Поисковый запрос");

    public SearchComponent() {
        mainLayout = CreateUI.getMainLayout();
        mainLayout.add(CreateUI.getTopLayout("Система поиска", "xl", null));
        mainLayout.add(createSearchComponent());
        requestTextField.setSizeFull();
    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    public static void setDataAccess(PageRepository pRepository,
                                     SiteRepository sRepository,
                                     LemmaRepository lRepository,
                                     PartOfSpeechRepository posRepository) {
        pageRepository = pRepository;
        siteRepository = sRepository;
        lemmaRepository = lRepository;
        partOfSpeechRepository = posRepository;
    }

    private VerticalLayout createSearchComponent() {
        ComboBox<String> siteComboBox = new ComboBox<>("Сайт:");

        siteComboBox.setItems(query -> {
            return siteRepository.getSitesUrlFromPageTable(
                    PageRequest.of(query.getPage(), query.getPageSize())
            ).stream();
        });

        siteComboBox.addValueChangeListener(event -> {
            siteRepository.getSiteByUrl(event.getValue()).ifPresent(site -> {
                //------------------------------------------------------------------------------------------
//                List pages = entityManager.createQuery("from Page Where Site_Id = :siteId order by Path")
//                                        .setParameter("siteId", site.getId())
//                        .setMaxResults(10)
//                        .getResultList();
//                pageGrid.setItems(pages);
                //------------------------------------------------------------------------------------------

                Integer pageCount = pageRepository.countBySiteId(site.getId());
                Integer lemmaCount = lemmaRepository.countBySiteId(site.getId());

                pageCountTextField.setValue(new DecimalFormat("#,###").format(pageCount));
                lemmaCountTextField.setValue(new DecimalFormat("#,###").format(lemmaCount));


                List<String> excludeList = partOfSpeechRepository.findByInclude(false)
                        .stream()
                        .map(p -> p.getShortName())
                        .collect(Collectors.toList());
                Lemmatization lemmatizator = new Lemmatization(excludeList, null);
                HashMap<String, Integer> requestLemmas = lemmatizator.getLemmaCount(requestTextField.getValue());

                lemmaGrid.setItems(query -> lemmaRepository
                        .findByLemmaIn(
                                requestLemmas.keySet().stream().toList(),
                                PageRequest.of(query.getPage(), query.getPageSize(), Sort.by("frequency")))
                        .stream());
            });
        });

        pageCountTextField.setReadOnly(true);
        lemmaCountTextField.setReadOnly(true);
        var horizontalLayout = new HorizontalLayout(siteComboBox, pageCountTextField, lemmaCountTextField);
        horizontalLayout.setSizeUndefined();


        lemmaGrid.addColumn(Lemma::getFrequency).setHeader("Частота")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.CENTER);
        lemmaGrid.addColumn(Lemma::getLemma).setHeader("Лемма")
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.START);


        requestTextField.setSizeUndefined();
        return new VerticalLayout(horizontalLayout, requestTextField, lemmaGrid);
    }
}
