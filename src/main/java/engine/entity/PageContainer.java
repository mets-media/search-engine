package engine.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@RequiredArgsConstructor
public class PageContainer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer siteId;
    private String path;
    private Integer code;
    @Column(columnDefinition = "Text")
    private String content;
    @Column(columnDefinition = "Text")
    private String lemmatization;
}
