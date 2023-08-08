package searchengine.config;

import lombok.*;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SiteConfig {
    private String url;
    private String name;
}
