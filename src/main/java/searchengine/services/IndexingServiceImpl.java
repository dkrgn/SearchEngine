package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.SiteModel;
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
        for (Site site : sitesList.getSites()) {
            deleteFromDB(site);
            addSite(site);
            IndexingThread thread = new IndexingThread(site, pageRepository, siteRepository, lemmaServiceImpl);
            threads.add(thread);
        }
        threads.forEach(pool::execute);
        indexing = true;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse stopIndex() {
        if (!indexing) {
            return new IndexingResponse(false, "Индексация не запущена");
        }
        pool.shutdown();
        sitesList.getSites().forEach(
                s -> siteRepository.changeStatusByUrl(s.getUrl(), Status.FAILED.name()));
        indexing = false;
        return new IndexingResponse(true);
    }

    @Override
    public IndexingResponse indexSinglePage(String url) {
        Site site = null;
        boolean checkIfInConfig = false;
        for (Site s : sitesList.getSites()) {
            if (s.getUrl().equals(url)) {
                checkIfInConfig = true;
                site = new Site(s.getUrl(), s.getName());
            }
        }
        if (checkIfInConfig) {
            deleteFromDB(site);
            addSite(site);
            IndexingThread thread = new IndexingThread(site, pageRepository, siteRepository, lemmaServiceImpl);
            pool.execute(thread);
            indexing = true;
            return new IndexingResponse(true);
        }
        return new IndexingResponse(false,
                "Данная страница находится за пределами сайтов, " +
                        "указанных в конфигурационном файле.");
    }

    private void addSite(Site site) {
        SiteModel model = new SiteModel();
        model.setDateTime(LocalDateTime.now());
        model.setLastError(null);
        model.setName(site.getName());
        model.setStatus(Status.INDEXING);
        model.setUrl(site.getUrl());
        siteRepository.save(model);
    }

    private void deleteFromDB(Site site) {
        if (siteRepository.getSiteIdByURL(site.getUrl()).isPresent()) {
            if (siteRepository.getSiteIdByURL(site.getUrl()).get().getStatus().equals(Status.INDEXED) ||
                    siteRepository.getSiteIdByURL(site.getUrl()).get().getStatus().equals(Status.FAILED))
                siteRepository.deleteById(siteRepository.getSiteIdByURL(site.getUrl()).get().getId());
        }
    }
}
