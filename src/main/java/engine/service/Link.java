package engine.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class Link implements Serializable {
    private String Parent;
    private String html;
    private Integer statusCode;
}
