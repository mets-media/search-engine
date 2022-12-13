package engine.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@RequiredArgsConstructor
public class PageContainer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(nullable = false)
    private Integer siteId;
    @Column(nullable = false)
    private String path;
    @Column(nullable = false)
    private Integer code;
    @Column(columnDefinition = "Text")
    private String content;
    @Column(columnDefinition = "Text")
    private String lemmatization;
}
