package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private final SiteRepository siteRepository;

    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> siteList = siteRepository.findAll();

        for (Site site : siteList) {
            total.setPages(total.getPages() + site.getPageList().size());
            total.setLemmas(total.getLemmas() + site.getLemmaList().size());

            DetailedStatisticsItem statisticsItem = getDetailedStatisticsItem(site);
            detailed.add(statisticsItem);
        }

        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);

        StatisticsResponse response = new StatisticsResponse();
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }

    private DetailedStatisticsItem getDetailedStatisticsItem (Site site) {

        DetailedStatisticsItem item = new DetailedStatisticsItem();
        ZonedDateTime zdt = ZonedDateTime.of(site.getStatusTime(), ZoneId.systemDefault());
        long date = zdt.toInstant().toEpochMilli();

        item.setName(site.getName());
        item.setUrl(site.getUrl());
        item.setPages(site.getPageList().size());
        item.setLemmas(site.getLemmaList().size());
        item.setStatus(site.getStatus().name());
        item.setError(site.getLastError());
        item.setStatusTime(date);

        return item;
    }
}
