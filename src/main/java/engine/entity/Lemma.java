package engine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.persistence.Index;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@Table(indexes = {@Index(name="site_idx",columnList = "siteId")},
        uniqueConstraints={@UniqueConstraint(name = "siteId_lemma_unique",columnNames={"siteId", "lemma"})})


//@Table(indexes = {@Index(name = "siteIdx", columnList="siteId"),
//                @Index(name="siteId_lemma_unique", columnNames = {"siteId", "lemma"}, unique = true)})
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private Integer siteId;
    @NotNull
    private String lemma;
    @NotNull
    @ColumnDefault("0")
    private Integer frequency;
/*
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "lemmaList")
    private Set<Page> pages = new HashSet<>();
*/
}
