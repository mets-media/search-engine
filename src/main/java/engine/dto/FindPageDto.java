package engine.dto;

public record FindPageDto(String site, String siteName, String uri, String title, String snippet, Float relevance) {
}
