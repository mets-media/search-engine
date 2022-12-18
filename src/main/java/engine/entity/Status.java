package engine.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Data
@RequiredArgsConstructor
public class Status {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private Integer siteId;
    @NotNull
    private Enum<SiteStatus> status;
    @NotNull
    private LocalDateTime statusTime;
    @Column(columnDefinition = "Text")
    private String message;

    public Status(Integer siteId, SiteStatus status, String message) {
        this.siteId = siteId;
        this.status = status;
        this.message = message;
        this.statusTime = LocalDateTime.now();
    }

}
