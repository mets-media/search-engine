package engine.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class SQLContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    @Column(columnDefinition = "Text", unique = true)
    private String name;
    @NotNull
    @Column(columnDefinition = "Text")
    private String param;
    @NotNull
    @Column(columnDefinition = "Text")
    private String sql;
    @Column(columnDefinition = "Text")
    private String note;
}
