package searchengine.controllers;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SearchService;
import searchengine.services.SiteIndexingService;
import searchengine.services.StatisticsService;

import java.util.Map;

import static java.util.Collections.singletonMap;

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
        ResponseEntity<String> result;
        if (siteIndexingService.indexingIsStarted()) {
            result = resultError("Индексация уже запущена");
        } else {
            result = resultOK();
        }
        return result;
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        ResponseEntity<String> result;
        if (siteIndexingService.indexingIsStopped()) {
            result = resultError("Индексация не запущена");
        } else {
            result = resultOK();
        }
        return result;
    }

    @PostMapping("/indexPage")
    public ResponseEntity<String> indexPage(@RequestParam String url) {
        ResponseEntity<String> result;
        if (siteIndexingService.indexPage(url)) {
            result = resultError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        } else {
            result = resultOK();
        }
        return result;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) String query,
                                    @RequestParam(required = false) String site,
                                    @RequestParam(defaultValue = "0") int offset,
                                    @RequestParam(defaultValue = "3") int limit) {
        ResponseEntity<?> result;
        if (query == null || query.isBlank()) {
            result = resultError("Задан пустой поисковой запрос");
        }else {
            result = new ResponseEntity<>(searchService.getSearchResults(query, site, offset, limit), HttpStatus.OK);
        }

        return result;
    }

    private ResponseEntity<String> resultError(String message) {
        return new ResponseEntity<>(generateResponse(singletonMap("result", false),
                singletonMap("error", message)), HttpStatus.OK);
    }

    private ResponseEntity<String> resultOK() {
        return new ResponseEntity<>(generateResponse(singletonMap("result", true)), HttpStatus.OK);
    }

    @SafeVarargs
    private String generateResponse(Map<Object, Object>... params) {
        JSONObject JSONObject = new JSONObject();
        for (Map<Object, Object> param : params) {
            JSONObject.putAll(param);
        }
        return JSONObject.toString();
    }

}
