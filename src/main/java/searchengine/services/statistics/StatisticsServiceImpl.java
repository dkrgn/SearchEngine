package searchengine.services.statistics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
        total.setSites(sites.getSiteConfigs().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteConfig> sitesList = sites.getSiteConfigs();
        long statusTime = System.currentTimeMillis();
        int pages = 0;
        int lemmas = 0;
        int totalPages = 0;
        int totalLemmas = 0;
        for (SiteConfig siteConfig : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteConfig.getName());
            item.setUrl(siteConfig.getUrl());
            if (siteRepository.getSiteIdByURL(siteConfig.getUrl()).isPresent()) {
                Site site = siteRepository.getSiteIdByURL(siteConfig.getUrl()).get();
                if (pageRepository.getAllBySiteId(site.getId()).isPresent()) {
                    pages = pageRepository.getAllBySiteId(site.getId()).get().size();
                    totalPages += pages;
                    item.setPages(pages);
                }
                if (lemmaRepository.getAllBySiteId(site.getId()).isPresent()) {
                    lemmas = lemmaRepository.getAllBySiteId(site.getId()).get().size();
                    totalLemmas += lemmas;
                    item.setLemmas(lemmas);
                }
                item.setStatus(site.getStatus().name());
                item.setError(site.getLastError());
                ZonedDateTime zdt = ZonedDateTime.of(site.getDateTime(), ZoneId.systemDefault());
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
