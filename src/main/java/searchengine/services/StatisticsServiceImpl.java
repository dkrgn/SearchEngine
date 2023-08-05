package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteModel;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        long statusTime = System.currentTimeMillis();
        int pages = 0;
        int lemmas = 0;
        int totalPages = 0;
        int totalLemmas = 0;
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            if (siteRepository.getSiteIdByURL(site.getUrl()).isPresent()) {
                SiteModel siteModel = siteRepository.getSiteIdByURL(site.getUrl()).get();
                if (pageRepository.getAllBySiteId(siteModel.getId()).isPresent()) {
                    pages = pageRepository.getAllBySiteId(siteModel.getId()).get().size();
                    totalPages += pages;
                    item.setPages(pages);
                }
                if (lemmaRepository.getAllBySiteId(siteModel.getId()).isPresent()) {
                    lemmas = lemmaRepository.getAllBySiteId(siteModel.getId()).get().size();
                    totalLemmas += lemmas;
                    item.setLemmas(lemmas);
                }
                item.setStatus(siteModel.getStatus().name());
                item.setError(siteModel.getLastError());
                ZonedDateTime zdt = ZonedDateTime.of(siteModel.getDateTime(), ZoneId.systemDefault());
                statusTime = zdt.toInstant().toEpochMilli();
            } else {
                item.setPages(pages);
                item.setLemmas(lemmas);
                item.setStatus(Status.FAILED.name());
                item.setError("Ошибка, сайт еще не проиндексирован");
            }
            item.setStatusTime(statusTime);
            total.setPages(totalPages);
            total.setLemmas(totalLemmas);
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
