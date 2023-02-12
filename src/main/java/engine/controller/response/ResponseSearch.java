package engine.controller.response;

import engine.dto.FindPageDto;
import lombok.Getter;

import java.util.List;

@Getter
public class ResponseSearch {
    final Boolean result = true;
    final Integer count;
    final List<FindPageDto> data;

    public ResponseSearch(Integer count, List<FindPageDto> data) {
        this.count = count;
        this.data = data;
    }

}
