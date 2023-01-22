package engine.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@RequiredArgsConstructor
public class PathTable {
    private Integer pageId;
    private Float absRelevance;
    private Float relRelevance;
    private String path;

    public PathTable(Integer pageId, Float absRelevance, Float relRelevance, String path) {
        this.pageId = pageId;
        this.absRelevance = absRelevance;
        this.relRelevance = relRelevance;
        this.path = path;
    }
}
