package engine.entity;


import engine.enums.SiteStatus;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
public class Site implements Serializable {
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
    @ColumnDefault("0")
    private Integer IndexCount;
    @NotNull
    @ColumnDefault("0")
    private Integer lemmaCount;
    @NotNull
    private LocalDateTime statusTime;
    @Column(columnDefinition = "Text")
    private String lastError;
}
