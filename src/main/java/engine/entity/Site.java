package engine.entity;


import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Data
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private String name;
    @NotNull
    private String url;
    @NotNull
    private Enum<SiteStatus> status;
    @NotNull
    @ColumnDefault("0")
    private Integer pageCount;
    @NotNull
    private LocalDateTime statusTime;
    @Column(columnDefinition = "Text")
    private String lastError;


}
