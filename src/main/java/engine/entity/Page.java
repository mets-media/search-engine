package engine.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
//@Table(indexes = @Index(name = "path_IDX", columnList = "SubString(path from 1 for 50)"))
public class Page implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(columnDefinition = "bigint not null default 0")
    private Integer siteId;
    @Column(columnDefinition = "Text not Null Unique")
    private String path;
    @Column(nullable = false)
    private Integer code;
    @Column(columnDefinition = "Text not null")
    private String content;

    public Page(int siteId, String path, Integer code, String content) {
        this.path = path;
        this.siteId = siteId;
        this.code = code;
        this.content = content;
    }
}
