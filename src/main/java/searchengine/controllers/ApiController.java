package searchengine.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexing.IndexingService;
import searchengine.services.search.SearchServiceImpl;
import searchengine.services.statistics.StatisticsService;

@AllArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

    private final SearchServiceImpl searchServiceImpl;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping ("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexingService.startIndex());
    }

    @GetMapping ("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexingService.stopIndex());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(
            @RequestParam String url) {
        return ResponseEntity.ok(indexingService.indexSinglePage(url));
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam("query") String query,
            @RequestParam(value = "site", defaultValue = "all") String site,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(searchServiceImpl.search(query, site, offset, limit));
    }
}
