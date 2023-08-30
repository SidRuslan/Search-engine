package searchengine.services;

import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.indexing.StopIndexingResponse;


public interface StartAndStopIndexingService {
    StartIndexingResponse startIndexing();
    StopIndexingResponse stopIndexing();
}
