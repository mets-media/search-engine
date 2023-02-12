package engine.controller.response;

import engine.dto.SiteInfoDto;
import engine.dto.TotalDto;
import engine.enums.SiteStatus;
import lombok.Getter;

import java.util.List;

@Getter
public class StatisticsWithErrors {
    private final boolean result = true;
    private final TotalDto total;
    private final List<SiteInfoDto> detailed;

    public StatisticsWithErrors(List<SiteInfoDto> detailed) {

        this.detailed = detailed;

        int sites = detailed.size();
        int pages = 0;
        int lemmas = 0;
        boolean isIndexing = true;

        for (SiteInfoDto siteInfo : detailed) {
            pages += siteInfo.pages();
            lemmas += siteInfo.lemmas();
            if (siteInfo.status() != SiteStatus.INDEXED) isIndexing = false;
        }
        this.total = new TotalDto(sites, pages, lemmas, isIndexing);
    }
}

