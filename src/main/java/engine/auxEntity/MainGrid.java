package engine.auxEntity;

import lombok.*;
import org.jboss.jandex.Main;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@Getter
@Setter
@Table(name="MainGrid")
@AllArgsConstructor
@NoArgsConstructor
public class MainGrid implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 50)
    private String fieldName;
    private Enum fieldType;
    @Column(length = 50)
    private String caption;
    private Integer width;
    @Column(length = 12)
    private String prefix;
    private Boolean readOnly;


}
