package searchengine.services;

import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.indexing.SinglePageIndexingResponse;
import searchengine.model.*;
import searchengine.repositories.IndexModelRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class SinglePageIndexingServiceImpl implements SinglePageIndexingService {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexModelRepository indexModelRepository;

    private final SitesList sitesList;

    @Override
    public SinglePageIndexingResponse addOrUpdatePageIndex(String url) {
        SinglePageIndexingResponse singlePageIndexingResponse = new SinglePageIndexingResponse();
        Site site = getSite(url);
        if (site == null ) {
            singlePageIndexingResponse.setResult(false);
            return singlePageIndexingResponse;
        }
        try {
            Page page = createNewPage(site, url);
            createLemmasAndIndexes(site, page);
        } catch (IOException e) {
            singlePageIndexingResponse.setResult(false);
            return singlePageIndexingResponse;
        }
        singlePageIndexingResponse.setResult(true);
        return singlePageIndexingResponse;
    }

    private Site getSite(String url) {
        String regexUrl = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        if(url.isBlank() || !url.matches(regexUrl)) {
            return null;
        }
        int lastSymbol = url.indexOf("/", 8);
        String rootUrl = url.substring(0, lastSymbol) + "/";
        Site site = siteRepository.findByUrl(rootUrl);
        if (site != null) {
            return site;
        }
        for (SiteConfig siteConfig : sitesList.getSites()) {
            if (rootUrl.equals(siteConfig.getUrl())) {
                Site newSite = new Site();
                newSite.setUrl(siteConfig.getUrl());
                newSite.setName(siteConfig.getName());
                newSite.setStatusTime(LocalDateTime.now());
                newSite.setStatus(IndexingStatus.INDEXING);
                siteRepository.save(newSite);
                return newSite;
            }
        }
        return null;
    }


    private Page createNewPage (Site site, String url) throws IOException {
        Page page = new Page();
        Document document = Jsoup.connect(url).get();
        int lastSymbol = url.indexOf("/", 8);
        String urlWithoutRoot = url.substring(lastSymbol);
        Page pageFromDb = pageRepository.findByPath(urlWithoutRoot);
        if(pageFromDb != null) {
            pageRepository.delete(pageFromDb);
        }
        page.setSite(site);
        page.setPath(url.replaceAll(site.getUrl(), "/"));
        page.setCode(document.connection().response().statusCode());
        page.setContent(document.html());
        pageRepository.save(page);
        return page;
    }

    private void createLemmasAndIndexes(Site site, Page page) throws IOException {
        LemmaFinder lemmaFinder = new LemmaFinder();
        HashMap<String, Integer> lemmas = lemmaFinder.getLemmasCollection(page.getContent());
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            IndexModel indexModel = new IndexModel();
            Lemma lemma = new Lemma();
            Lemma lemmaFromDb = lemmaRepository.findByLemmaAndSite_Id(entry.getKey(), site.getId());
            if(lemmaFromDb != null) {
                lemmaFromDb.setFrequency(lemmaFromDb.getFrequency() + 1);
                lemmaRepository.save(lemmaFromDb);
                indexModel.setLemma(lemmaFromDb);
            }else {
                lemma.setSite(site);
                lemma.setLemma(entry.getKey());
                lemma.setFrequency(1);
                lemmaRepository.save(lemma);
                indexModel.setLemma(lemma);
            }
            indexModel.setPage(page);
            indexModel.setRank(entry.getValue());
            indexModelRepository.save(indexModel);

        }
    }

}
