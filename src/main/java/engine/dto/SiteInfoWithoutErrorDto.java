package engine.dto;

import engine.enums.SiteStatus;
import java.time.LocalDateTime;

public record SiteInfoWithoutErrorDto(String url,
                                      String name,
                                      Enum<SiteStatus> status,
                                      LocalDateTime statusTime,
                                      Integer pages,
                                      Integer lemmas) {

}