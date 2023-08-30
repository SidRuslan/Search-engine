package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.ErrorResponse;
import searchengine.dto.indexing.SinglePageIndexingResponse;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.indexing.StopIndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.*;
import searchengine.services.SinglePageIndexingService;
import searchengine.services.SearchService;
import searchengine.services.StartAndStopIndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final StartAndStopIndexingService startAndStopIndexingService;
    private final SinglePageIndexingService indexPageService;
    private final SearchService searchService;


    @Autowired
    public ApiController(StatisticsService statisticsService, StartAndStopIndexingService startIndexingService, SinglePageIndexingService indexPageService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.startAndStopIndexingService = startIndexingService;
        this.indexPageService = indexPageService;
        this.searchService = searchService;
    }


    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }


    @GetMapping("/startIndexing")
    public ResponseEntity startIndexing() {
        StartIndexingResponse startIndexingResponse = startAndStopIndexingService.startIndexing();
        if (startIndexingResponse.isResult()) {
            return ResponseEntity.ok(startIndexingResponse);
        }
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("Индексация уже запущена");
        return ResponseEntity.badRequest().body(errorResponse);
    }


    @GetMapping("/stopIndexing")
    public ResponseEntity stopIndexing() {
        StopIndexingResponse stopIndexingResponse = startAndStopIndexingService.stopIndexing();
        if(stopIndexingResponse.isResult()) {
            return ResponseEntity.ok(stopIndexingResponse);
        }
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("Индексация не запущена");
        return ResponseEntity.badRequest().body(errorResponse);
    }


    @PostMapping("/indexPage")
    public ResponseEntity indexPage(@RequestParam String url) {
        SinglePageIndexingResponse singlePageIndexingResponse = indexPageService.addOrUpdatePageIndex(url);
        if(singlePageIndexingResponse.isResult()) {
            return ResponseEntity.ok(singlePageIndexingResponse);
        }
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        return ResponseEntity.badRequest().body(errorResponse);

    }

    @GetMapping("/search")
    public ResponseEntity search(String query, String site) {
        SearchResponse searchResponse = searchService.search(query, site);
        if(searchResponse.isResult()) {
            return ResponseEntity.ok(searchResponse);
        }
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("Задан пустой поисковый запрос");
        return ResponseEntity.badRequest().body(errorResponse);

    }


}
