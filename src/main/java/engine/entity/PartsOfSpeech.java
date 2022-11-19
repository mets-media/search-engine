package engine.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class PartsOfSpeech {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "varchar(50) unique")
    private String name;
    @NotNull
    @Column(columnDefinition = "varchar(12) unique")
    private String shortName;
    @NotNull
    @ColumnDefault("true")
    private Boolean include;

    public PartsOfSpeech(String shortName, Boolean include) {
        this.shortName = shortName;
        this.include = include;
    }
}
