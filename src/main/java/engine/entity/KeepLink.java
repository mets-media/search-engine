package engine.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.convert.ReadingConverter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@RequiredArgsConstructor
public class KeepLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    private Integer siteId;
    private Integer code;
    private Integer status;
    @NotNull
    @Column(columnDefinition = "Text")
    private String path;

    public KeepLink(int siteId, int code, int status, String path) {
        this.siteId = siteId;
        this.code = code;
        this.status = status;
        this.path = path;
    }
}
