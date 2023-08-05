package searchengine.services;

import searchengine.dto.statistics.IndexingResponse;

import java.util.List;

public interface IndexingService {

    IndexingResponse startIndex();

    IndexingResponse stopIndex();

    IndexingResponse indexSinglePage(String link);
}
