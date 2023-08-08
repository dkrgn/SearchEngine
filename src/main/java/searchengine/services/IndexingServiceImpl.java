package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SiteRepository siteRepository;

    private final PageRepository pageRepository;

    private final LemmaServiceImpl lemmaServiceImpl;

    private final ForkJoinPool pool = new ForkJoinPool();

    private final SitesList sitesList;

    private boolean indexing = false;

    @Override
    public IndexingResponse startIndex() {
        List<IndexingThread> threads = new ArrayList<>();
        for (SiteConfig siteConfig : sitesList.getSiteConfigs()) {
            if (checkIfIndexing(siteConfig.getUrl())) {
                return new IndexingResponse(false, "Индексация уже запущена");
            }
            deleteFromDB(siteConfig);
            addSite(siteConfig);
            Site site = siteRepository.getSiteIdByURL(siteConfig.getUrl()).get();
            IndexingThread thread = new IndexingThread(site, siteConfig.getUrl(), pageRepository, siteRepository, lemmaServiceImpl);
            threads.add(thread);
        }
        threads.forEach(pool::execute);
        indexing = true;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndex() {
        if (!indexing) {
            return new IndexingResponse(false, "Индексация уже запущена");
        }
        pool.shutdown();
        sitesList.getSiteConfigs().forEach(
                s -> siteRepository.changeStatusByUrl(s.getUrl(), Status.FAILED.name()));
        indexing = false;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse indexSinglePage(String url) {
        SiteConfig siteConfig = null;
        boolean checkIfInConfig = false;
        for (SiteConfig s : sitesList.getSiteConfigs()) {
            if (s.getUrl().equals(url)) {
                checkIfInConfig = true;
                siteConfig = new SiteConfig(s.getUrl(), s.getName());
            }
        }
        if (checkIfInConfig) {
            if (checkIfIndexing(siteConfig.getUrl())) {
                return new IndexingResponse(false, "Индексация уже запущена");
            }
            deleteFromDB(siteConfig);
            addSite(siteConfig);
            Site site = siteRepository.getSiteIdByURL(siteConfig.getUrl()).get();
            IndexingThread thread = new IndexingThread(site, siteConfig.getUrl(), pageRepository, siteRepository, lemmaServiceImpl);
            pool.execute(thread);
            indexing = true;
            return new IndexingResponse(true);
        }
        return new IndexingResponse(false,
                "Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле.");
    }

    private boolean checkIfIndexing(String url) {
        if (siteRepository.getSiteIdByURL(url).isPresent()) {
             return siteRepository.getSiteIdByURL(url).get().getStatus().equals(Status.INDEXING);
        }
        return false;
    }

    private void addSite(SiteConfig siteConfig) {
        Site model = new Site();
        model.setDateTime(LocalDateTime.now());
        model.setLastError(null);
        model.setName(siteConfig.getName());
        model.setStatus(Status.INDEXING);
        model.setUrl(siteConfig.getUrl());
        siteRepository.save(model);
    }

    private void deleteFromDB(SiteConfig siteConfig) {
        if (siteRepository.getSiteIdByURL(siteConfig.getUrl()).isPresent()) {
            if (siteRepository.getSiteIdByURL(siteConfig.getUrl()).get().getStatus().equals(Status.INDEXED) ||
                    siteRepository.getSiteIdByURL(siteConfig.getUrl()).get().getStatus().equals(Status.FAILED))
                siteRepository.deleteById(siteRepository.getSiteIdByURL(siteConfig.getUrl()).get().getId());
        }
    }
}
