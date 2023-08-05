package searchengine.services;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

@Service
@AllArgsConstructor
public class IndexingThread extends RecursiveTask<IndexingResponse> {

    private final Site site;

    private final PageRepository pageRepository;

    private final SiteRepository siteRepository;

    private final LemmaServiceImpl lemmaServiceImpl;

    @Override
    protected IndexingResponse compute() {
        long start = System.currentTimeMillis();
        Connection.Response response;
        try {
            if (ifIndexing()) {
                SiteModel siteModel = siteRepository.getSiteIdByURL(site.getUrl()).get();
                List<IndexingThread> tasks = new ArrayList<>();
                System.out.println("Site name: " + site.getName() + "\nLink: " + site.getUrl());
                response = Jsoup.connect(site.getUrl())
                        .timeout(3000)
                        .execute();
                Document doc = response.parse();
                Elements aTag = doc.getElementsByTag("a");
                int statusCode = response.statusCode();
                List<String> hrefs = new ArrayList<>();
                aTag.forEach(link -> hrefs.add(link.attr("href")));
                saveToDB(doc, statusCode, siteModel, site.getUrl());
                iterateLinks(hrefs, siteModel, tasks);
                tasks.forEach(ForkJoinTask::join);
                return ifSucceeded(start);
            } else {
                return new IndexingResponse(false, "Индексация уже запущена");
            }
        } catch (IOException e) {
            siteRepository.changeStatusByUrl(site.getUrl(), Status.FAILED.name());
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean ifIndexing() {
        if (siteRepository.getSiteIdByURL(site.getUrl()).isPresent()) {
            return siteRepository.getSiteIdByURL(site.getUrl()).get().getStatus().equals(Status.INDEXING);
        }
        return false;
    }

    private void saveToDB(Document doc, int statusCode, SiteModel siteModel, String link) {
        PageModel page = buildPage(doc, statusCode, link, siteModel);
        pageRepository.save(page);
        lemmaServiceImpl.buildLemmas(page.getContent(), siteModel);
        siteRepository.updateDateTime(siteModel.getId(), LocalDateTime.now());
    }

    private void iterateLinks(List<String> hrefs, SiteModel siteModel, List<IndexingThread> tasks) {
        for (String link : hrefs) {
            if (!link.isEmpty()) {
                if (checkIfContains(link, siteModel)) {
                    continue;
                }
                indexPageAndLemmas(tasks, link);
            }
        }
    }

    private boolean checkIfContains(String link, SiteModel siteModel) {
        return pageRepository.getByPathAndId(link, siteModel.getId()).isPresent();
    }

    private String ifStartsWithSlash(String link) {
        return link.startsWith("/") ? site.getUrl() + link : link;
    }

    private void indexPageAndLemmas(List<IndexingThread> tasks, String link) {
        Site site = new Site();
        site.setName(Thread.currentThread().getName());
        site.setUrl(ifStartsWithSlash(link));
        System.err.println(site);
        IndexingThread indexingThread = new IndexingThread(site, pageRepository, siteRepository, lemmaServiceImpl);
        indexingThread.fork();
        tasks.add(indexingThread);
    }

    private PageModel buildPage(Document doc, int status, String path, SiteModel siteModel) {
        PageModel pageModel = new PageModel();
        pageModel.setCode(status);
        pageModel.setContent(doc.html());
        pageModel.setPath(path);
        pageModel.setSiteModel(siteModel);
        return pageModel;
    }

    private IndexingResponse ifSucceeded(long start) {
        if (pageRepository.getAll().isPresent()) {
            System.err.println(System.currentTimeMillis() - start);
            siteRepository.changeStatusByUrl(site.getUrl(), Status.INDEXED.name());
            return new IndexingResponse(true);
        }
        return new IndexingResponse(false, "Ошибка индексации");
    }
}
