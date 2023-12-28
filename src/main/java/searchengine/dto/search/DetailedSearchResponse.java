package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetailedSearchResponse {

    private String siteUrl;
    private String siteName;
    private String pageUri;
    private String pageTitle;
    private String snippet;
    private float relevance;
}
