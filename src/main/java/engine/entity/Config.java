package engine.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(columnDefinition = "varchar(5) not null Unique")
    private String key;
    private String name;
    private String value;

    public Config(String key, String name, String value) {
        this.key = key;
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }
}
