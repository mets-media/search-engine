package engine.config;

import engine.entity.Site;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@Getter
@Setter
public class YAMLConfig {
    private List<Site> sites = new ArrayList<>();
    private String userAgent;
    private String referrer;
    private Integer timeout;
    private Boolean autoScan;
    private Integer delay;
}
