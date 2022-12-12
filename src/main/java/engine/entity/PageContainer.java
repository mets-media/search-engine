package engine.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageContainer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(columnDefinition = "bigint", nullable = false)
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