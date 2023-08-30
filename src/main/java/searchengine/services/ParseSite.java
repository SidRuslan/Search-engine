package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import searchengine.model.IndexModel;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexModelRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveTask;



@RequiredArgsConstructor
public class ParseSite extends RecursiveTask<TreeSet<Page>> {

    private static PageRepository pageRepository;
    private static LemmaRepository lemmaRepository;
    private static IndexModelRepository indexModelRepository;

    private final Site site;
    private final String url;
    private static final TreeSet<String> linksList = new TreeSet<>();
    private static final TreeSet<Page> pagesSet = new TreeSet<>();
    private volatile static boolean isStoppedIndexing;


    @Override
    protected TreeSet<Page> compute() {
        try {
            if (isIsStoppedIndexing()) {
                Thread.sleep(100);
                throw new InterruptedException();
            }
            List<ParseSite> taskList = new ArrayList<>();
            if (!linksList.contains(url)) {
                linksList.add(url);
                Thread.sleep(500);
                Document document = getDocument(url);
                System.out.println(document.baseUri());
                createPage(document, site);
                Elements elements = document.select("a[href]");
                for (Element element : elements) {
                    String link = element.attr("abs:href");
                    if (isCorrectLink(link)) {
                        ParseSite parseSite = new ParseSite(site, link);
                        parseSite.fork();
                        taskList.add(parseSite);
                    }
                }
                for (ParseSite task : taskList) {
                    task.join();
                }
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException("Индексация остановлена пользователем");
        } catch (IOException e) {
            throw new RuntimeException("Ошибка индексикации: страница сайта недоступна");
        }
        return pagesSet;
    }


    private void createPage(Document document, Site site) throws IOException {

        Page page = new Page();
        page.setContent(document.html());
        page.setCode(document.connection().response().statusCode());
        page.setSite(site);
        page.setPath(document.baseUri().replaceAll(site.getUrl(), "/"));

        pagesSet.add(page);
        pageRepository.save(page);

        site.setStatusTime(LocalDateTime.now());
        createLemmaAndIndex(page, site);
    }


    private void createLemmaAndIndex(Page page, Site site) throws IOException {
        LemmaFinder lemmaFinder = new LemmaFinder();
        HashMap<String, Integer> lemmas = lemmaFinder.getLemmasCollection(page.getContent());
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            IndexModel indexModel = new IndexModel();
            Lemma lemma = new Lemma();
            try {
                Lemma lemmaFromDb = lemmaRepository.findByLemmaAndSite_Id(entry.getKey(), site.getId());
                if (lemmaFromDb != null && lemmaFromDb.getSite().getId().equals(site.getId())) {
                    lemmaFromDb.setFrequency(lemmaFromDb.getFrequency() + 1);
                    lemmaRepository.save(lemmaFromDb);
                    indexModel.setLemma(lemmaFromDb);
                } else {
                    lemma.setSite(site);
                    lemma.setLemma(entry.getKey());
                    lemma.setFrequency(1);
                    lemmaRepository.save(lemma);
                    indexModel.setLemma(lemma);
                }
                indexModel.setPage(page);
                indexModel.setRank(entry.getValue());
                indexModelRepository.save(indexModel);
            } catch (IncorrectResultSizeDataAccessException ex) {
                ex.getMessage();
            }
        }
    }


    private boolean isCorrectLink (String link) {
        return link.startsWith(site.getUrl()) && !linksList.contains(link) &&
                !link.contains("#") && !link.contains("?") &&
                !link.contains(".jpg") && !link.contains(".pdf") &&
                !link.contains(".xlsx") && !link.contains(".doc") &&
                !link.contains(".ppt") && !link.contains(".m") &&
                !link.contains(".fiq") && !link.contains(".png");
    }

    private Document getDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6)" +
                        " Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com").get();
    }


    public static void setPageRepository(PageRepository pageRepository) {
        ParseSite.pageRepository = pageRepository;
    }

    public static void setLemmaRepository(LemmaRepository lemmaRepository) {
        ParseSite.lemmaRepository = lemmaRepository;
    }

    public static void setIndexModelRepository(IndexModelRepository indexModelRepository) {
        ParseSite.indexModelRepository = indexModelRepository;
    }

    public static boolean isIsStoppedIndexing() {
        return isStoppedIndexing;
    }

    public static void setIsStoppedIndexing(boolean isStoppedIndexing) {
        ParseSite.isStoppedIndexing = isStoppedIndexing;
    }
}


