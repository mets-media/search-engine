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

    public PathTable(String path, Float absRelevance, Float relRelevance) {
        this.path = path;
        this.absRelevance = absRelevance;
        this.relRelevance = relRelevance;
    }
}