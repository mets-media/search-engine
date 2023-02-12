package engine.dto;

import engine.enums.SiteStatus;

import java.time.LocalDateTime;

public record SiteInfoDto(String url,
                          String name,
                          Enum<SiteStatus> status,
                          LocalDateTime statusTime,
                          String error,
                          Integer pages,
                          Integer lemmas) {
}
