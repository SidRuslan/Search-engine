package searchengine.services;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.indexing.StopIndexingResponse;
import searchengine.model.*;
import searchengine.repositories.IndexModelRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;


@Service
@AllArgsConstructor
public class StartAndStopIndexingServiceImpl implements StartAndStopIndexingService{

    private final SitesList sitesList;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexModelRepository indexModelRepository;

    private static ForkJoinPool forkJoinPool;


    @Override
    public StartIndexingResponse startIndexing() {
        StartIndexingResponse startIndexingResponse = new StartIndexingResponse();
        forkJoinPool = new ForkJoinPool();
        try {
            for (SiteConfig siteConfig : sitesList.getSites()) {
                new Thread(() -> createSite(siteConfig)).start();
            }
        } catch (Exception ex) {
            startIndexingResponse.setResult(false);
            return startIndexingResponse;
        }
        startIndexingResponse.setResult(true);
        return startIndexingResponse;

    }

    @Override
    public StopIndexingResponse stopIndexing() {
        StopIndexingResponse stopIndexingResponse = new StopIndexingResponse();
        try {
            ParseSite.setIsStoppedIndexing(true);
            forkJoinPool.shutdownNow();
        }catch (Exception ex) {
            stopIndexingResponse.setResult(false);
        }
        stopIndexingResponse.setResult(true);
        return stopIndexingResponse;
    }


    private void createSite(SiteConfig siteConfig) {

        deleteSiteByName(siteConfig.getName());

        Site site = new Site();
        site.setUrl(siteConfig.getUrl());
        site.setName(siteConfig.getName());
        site.setStatusTime(LocalDateTime.now());
        site.setStatus(IndexingStatus.INDEXING);
        siteRepository.save(site);

        createPages(site);
    }


    private void createPages(Site site) {
        try {
            ParseSite.setPageRepository(pageRepository);
            ParseSite.setLemmaRepository(lemmaRepository);
            ParseSite.setIndexModelRepository(indexModelRepository);

            ParseSite parseSite = new ParseSite(site, site.getUrl());
            ParseSite.setIsStoppedIndexing(false);
            forkJoinPool.invoke(parseSite);

            site.setStatusTime(LocalDateTime.now());
            site.setStatus(IndexingStatus.INDEXED);
            siteRepository.save(site);
        } catch (Exception ex) {
            site.setStatus(IndexingStatus.FAILED);
            site.setLastError(ex.getMessage().replaceAll("[a-zA-Z.:]", "").trim());
            siteRepository.save(site);
        }

    }


    private void deleteSiteByName(String name) {
        List<Site> sites = siteRepository.findAllByName(name);
        for (Site site : sites) {
            if (site != null) {
                siteRepository.delete(site);
            }
        }
    }

}
