package searchengine.dto.search;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchResponse {

    private boolean result;
    private int count;
    private List<DetailedSearchResponse> detailedSearchResponseList;
    private String error;

    public SearchResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public SearchResponse(boolean result, int count, List<DetailedSearchResponse> detailedSearchResponseList) {
        this.result = result;
        this.count = count;
        this.detailedSearchResponseList = detailedSearchResponseList;
    }
}
