package searchengine.services;

import lombok.AllArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.search.SearchQueryResult;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexModel;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexModelRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.SiteRepository;


import java.io.IOException;
import java.util.*;

@Service
@AllArgsConstructor
public class SearchServiceImpl implements SearchService{

    private static final int maxNumberOfPages = 50;

    private final SitesList sitesList;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private IndexModelRepository indexModelRepository;


    @Override
    public SearchResponse search(String query, String siteUrl) {
            if (query.isEmpty()) {
                return createSearchResponse(new ArrayList<>(), false);
            }
            LemmaFinder lemmaFinder = new LemmaFinder();
            List<String> lemmas = lemmaFinder.getLemmasCollection(query).keySet().stream().toList();

            if(siteUrl == null) {
                List<Lemma> sortedLemmasList = new ArrayList<>();
                List<Page> pageList = new ArrayList<>();
                for (SiteConfig siteConfig : sitesList.getSites()) {
                    List<Lemma> lemmaList = new ArrayList<>(getSortedLemmaList(siteConfig.getUrl(), lemmas));
                    pageList.addAll(findPagesWithLemmas(lemmaList));
                    sortedLemmasList.addAll(lemmaList);
                }
                return createSearchResponse(createSearchQueryResult(pageList, sortedLemmasList), true);
            }
            List<Lemma> sortedLemmasList = getSortedLemmaList(siteUrl, lemmas);
            List<Page> pageList = findPagesWithLemmas(sortedLemmasList);
            return createSearchResponse(createSearchQueryResult(pageList, sortedLemmasList), true);
    }


    private List<Lemma> getSortedLemmaList (String siteUrl, List<String> lemmas) {

        List<Lemma> sortedLemmasList = new ArrayList<>();
        for (String lemma : lemmas) {
            Site site = siteRepository.findByUrl(siteUrl);
            Lemma foundLemma = lemmaRepository.findByLemmaAndSite_Id(lemma, site.getId());
            if (foundLemma != null && foundLemma.getFrequency() < maxNumberOfPages) {
                sortedLemmasList.add(foundLemma);
            }
        }
        sortedLemmasList.sort(Comparator.comparingInt(Lemma::getFrequency));

        if (sortedLemmasList.size() < lemmas.size()) {
            sortedLemmasList.clear();
            return sortedLemmasList;
        }

        return sortedLemmasList;
    }


    private List<Page> findPagesWithLemmas (List<Lemma> lemmaList) {
        if(lemmaList.isEmpty()) {
            return new ArrayList<>();
        }
        List<Page> pageList = new ArrayList<>(lemmaList.get(0).getPages());
        if(lemmaList.size() == 1) {
            return pageList;
        }
        for (Page page : pageList) {
            for (Lemma lemma : lemmaList) {
                if(!page.getLemmaList().contains(lemma)) {
                    pageList.remove(page);
                }
            }
        }
        return pageList;
    }


    private List<SearchQueryResult> createSearchQueryResult (List<Page> pageList, List<Lemma> sortedLemmasList) {

        float maxRelevance = getMaxRelevance(pageList, sortedLemmasList);
        List<SearchQueryResult> queryResultsList = new ArrayList<>();

        for (Page page : pageList) {
            SearchQueryResult searchQueryResult = new SearchQueryResult();
            String url = page.getSite().getUrl();
            Document doc = Jsoup.parse(page.getContent());

            searchQueryResult.setSite(url.substring(0, url.length() - 1));
            searchQueryResult.setSiteName(page.getSite().getName());
            searchQueryResult.setUri(page.getPath());
            searchQueryResult.setTitle(doc.title());
            searchQueryResult.setSnippet(createSnippet(sortedLemmasList, doc));
            searchQueryResult.setRelevance(calculateRelevance(page,maxRelevance, sortedLemmasList));

            queryResultsList.add(searchQueryResult);
        }
        queryResultsList.sort(Comparator.comparingDouble(SearchQueryResult ::getRelevance).reversed());

        return queryResultsList;
    }


    private float calculateRelevance(Page page, float maxRelevance, List<Lemma> sortedLemmasList) {

        float absLemmaRelevance = 0;

        for (Lemma lemma : sortedLemmasList) {
            IndexModel indexModel = indexModelRepository.findByLemma_idAndPage_Id(lemma.getId(), page.getId());
            if (indexModel == null) {
                continue;
            }
            absLemmaRelevance += indexModel.getRank();
        }
        return absLemmaRelevance / maxRelevance;
    }


    private float getMaxRelevance (List<Page> pageList, List<Lemma> sortedLemmasList) {

        float absLemmaRelevance = 0;
        float maxRelevance = 0;

        for (Page page : pageList) {
            for (Lemma lemma : sortedLemmasList) {
                IndexModel indexModel = indexModelRepository.findByLemma_idAndPage_Id(lemma.getId(), page.getId());
                if (indexModel == null) {
                    continue;
                }
                absLemmaRelevance += indexModel.getRank();
            }
            if (maxRelevance < absLemmaRelevance) {
                maxRelevance = absLemmaRelevance;
            }
            absLemmaRelevance = 0;
        }
        return maxRelevance;
    }


    private SearchResponse createSearchResponse (List<SearchQueryResult> queryResultsList, Boolean isResult) {
        SearchResponse searchResponse = new SearchResponse();
        if(!isResult) {
            searchResponse.setResult(false);
            return searchResponse;
        }
        searchResponse.setResult(true);
        searchResponse.setCount(queryResultsList.size());
        searchResponse.setData(queryResultsList);

        return searchResponse;
    }


    private String createSnippet(List<Lemma> sortedLemmasList, Document doc)  {

        String content = doc.text();
        List<String> contentWords = Arrays.stream(content.split("[ -]")).toList();
        Map<String, String> contentWordsWithLemmas = getContentWordsWithLemmas(contentWords);
        Map<String, Integer> oneIndexOfEachLemma = new TreeMap<>();
        List<Integer> allIndexesLemmas = new ArrayList<>();

        for (Lemma lemma : sortedLemmasList) {
            for (Map.Entry<String, String> WordWithLemma : contentWordsWithLemmas.entrySet()) {
                if (!WordWithLemma.getValue().equals(lemma.getLemma())) {
                    continue;
                }
                for (int i = 0; i < contentWords.size(); i++) {
                    if (contentWords.get(i).equals(WordWithLemma.getKey())) {
                        allIndexesLemmas.add(i);
                        oneIndexOfEachLemma.put(WordWithLemma.getValue(), i);
                    }
                }
            }
        }
        return getSnippet(contentWords, oneIndexOfEachLemma, allIndexesLemmas);
    }


    private Map<String, String> getContentWordsWithLemmas(List<String> contentWords) {

        Map<String, String> contentWordsWithLemmas = new HashMap<>();
        LuceneMorphology luceneMorph;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String regexWord = "[^А-Яа-я]";

        for (String word : contentWords) {
            if(word.length() < 2) {
                contentWordsWithLemmas.put(word, word);
                continue;
            }
            String wordWithoutSigns = word.toLowerCase().replaceAll(regexWord, "");
            if (wordWithoutSigns.isEmpty()) {
                contentWordsWithLemmas.put(word, word);
                continue;
            }
            List<String> wordBaseFormsList = luceneMorph.getNormalForms(wordWithoutSigns);
            if(wordBaseFormsList.isEmpty()) {
                contentWordsWithLemmas.put(word, word);
                continue;
            }
            String wordBaseForms = wordBaseFormsList.get(0);
            contentWordsWithLemmas.put(word, wordBaseForms);
        }
        return contentWordsWithLemmas;
    }


    private String getSnippet(List<String> contentWords,
                              Map<String, Integer> oneIndexOfEachLemma,
                              List<Integer> allIndexesLemmas) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Integer> repeatedLemmas = new ArrayList<>();
        int wordCount = 5;
        for (Map.Entry<String, Integer> indexLemma : oneIndexOfEachLemma.entrySet()) {
            if(oneIndexOfEachLemma.size() > 4) {
                wordCount = 3;
            }
            if(repeatedLemmas.contains(indexLemma.getValue())) {
                continue;
            }
            for (int i = indexLemma.getValue() - wordCount; i < indexLemma.getValue() + wordCount; i++) {
                if (i < 0 || i > contentWords.size()) {
                    continue;
                }
                if (i == indexLemma.getValue()) {
                    stringBuilder.append("<b>").append(contentWords.get(i)).append("</b>").append(" ");
                    continue;
                }
                if(allIndexesLemmas.contains(i)) {
                    stringBuilder.append("<b>").append(contentWords.get(i)).append("</b>").append(" ");
                    repeatedLemmas.add(i);
                    continue;
                }
                stringBuilder.append(contentWords.get(i)).append(" ");
            }
            stringBuilder.append("... ");
        }
        return stringBuilder.toString();
    }

}
