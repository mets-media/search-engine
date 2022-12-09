package engine.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Table(uniqueConstraints={@UniqueConstraint(name = "siteId_lemma_unique",columnNames={"siteId", "lemma"})})
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
    @NotNull
    @ColumnDefault("0")
    private Float rank;
}
