package engine.controller.response;

import engine.dto.TotalDto;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Statistics {
    private final boolean result = true;
    private TotalDto total;
    private final List<Object> detailed = new ArrayList<>();
}