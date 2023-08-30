package searchengine.services;

import searchengine.dto.indexing.SinglePageIndexingResponse;

public interface SinglePageIndexingService {
    SinglePageIndexingResponse addOrUpdatePageIndex(String url);
}
