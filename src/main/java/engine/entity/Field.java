package engine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private String name;
    @NotNull
    private String selector;
    @NotNull
    private Float weight;
    @NotNull
    @ColumnDefault("true")
    private boolean active;


    public Field(String name, String selector, Float weight) {
        this.name = name;
        this.selector = selector;
        this.weight = weight;
    }
}
