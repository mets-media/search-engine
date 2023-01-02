package engine.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.persistence.Index;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Data
@RequiredArgsConstructor
//@Table(indexes = @Index(name = "path_IDX", columnList = "SubString(path from 1 for 50)"))
@Table(indexes = {@Index(name = "siteId_idx", columnList = "siteId")},
        uniqueConstraints={@UniqueConstraint(name = "siteId_path_unique",columnNames={"siteId", "path"})})
public class Page implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqGenPage")
    //@SequenceGenerator(name = "seqGenPage", sequenceName = "seqPage", initialValue = 1)
    private Integer id;
    @NotNull
    private Integer siteId;
    @NotNull
    @Column(columnDefinition = "Text")
    private String path;
    @NotNull
    private Integer code;
    @Column(columnDefinition = "Text")
    private String content;

    public Page(Integer siteId, String path, Integer code, String content) {
        this.path = path;
        this.siteId = siteId;
        this.code = code;
        this.content = content;
    }

}
