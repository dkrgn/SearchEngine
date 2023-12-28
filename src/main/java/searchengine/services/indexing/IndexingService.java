package searchengine.services.indexing;

import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {

    IndexingResponse startIndex();

    IndexingResponse stopIndex();

    IndexingResponse indexSinglePage(String link);
}
