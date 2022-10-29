package engine.entity;


import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String url;
    private Enum<SiteStatus> status;
    private Integer pageCount;

}
