package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SearchService;
import searchengine.services.SiteIndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final StatisticsService statisticsService;

    @Autowired
    private SiteIndexingService siteIndexingService;
    @Autowired
    private SearchService searchService;

    public ApiController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() {
        return siteIndexingService.start();
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        return siteIndexingService.stop();
    }

    @PostMapping("/indexPage")
    public ResponseEntity<String> indexPage(@RequestParam String url) {
        return siteIndexingService.startIndexPage(url);
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query,
                                    @RequestParam(required = false) String site,
                                    @RequestParam(required = false, defaultValue = "0") Integer offset,
                                    @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return searchService.getResult(query, site, offset, limit);
    }
}
