package engine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Index;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Table(indexes = {@Index(name="site_idx",columnList = "siteId")},
        uniqueConstraints={@UniqueConstraint(name = "siteId_lemma_unique",columnNames={"siteId", "lemma"})})

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
}
