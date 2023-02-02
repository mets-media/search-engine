package engine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Table(name = "index",
        indexes = {@javax.persistence.Index(name = "pageId_idx", columnList = "pageId"),
                   @javax.persistence.Index(name = "lemmaId_idx", columnList = "lemmaId")})
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private Integer pageId;
    @NotNull
    private Integer lemmaId;
    @NotNull
    private float rank;
}
