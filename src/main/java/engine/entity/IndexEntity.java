package engine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
//@Table(name = "index",
//        indexes = @Index(name = "pageId_idx", columnList = "pageId"))
@Table(name = "index")
public class IndexEntity {
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
