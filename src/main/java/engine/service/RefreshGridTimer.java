package engine.service;

import com.vaadin.flow.component.grid.Grid;
import engine.entity.Site;
import engine.repository.SiteRepository;
import engine.views.MainView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;


public class RefreshGridTimer extends TimerTask {
    @Autowired
    SiteRepository siteRepository;
    private Grid<Site> grid;
    private Site site;
    private ConcurrentSkipListSet<String> readyLinks;

    //public RefreshGridTimer(Grid<Site> grid, Site site, ConcurrentSkipListSet<String> readyLinks) {

    public RefreshGridTimer(Grid<Site> grid) {
        this.grid = grid;
    }

    @Override
    public void run() {
//        site.setPageCount(readyLinks.size());
//        siteRepository.save(site);
//        grid.getDataProvider().refreshItem(site);
        MainView.gridRefresh();
    }
}
