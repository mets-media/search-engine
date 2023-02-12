package engine.dto;

public record QueryDto(String query, String site, Integer offset, Integer limit) {
}
