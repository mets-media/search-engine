package engine.entity;

import lombok.Data;
import org.springframework.data.convert.ReadingConverter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@ReadingConverter
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

}
