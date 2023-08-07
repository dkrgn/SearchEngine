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

@AllArgsConstructor
public class IndexingThread extends RecursiveTask<IndexingResponse> {

    private final SiteModel siteModel;

    private final Site site;

    private final PageRepository pageRepository;

    private final SiteRepository siteRepository;

    private final LemmaServiceImpl lemmaServiceImpl;

    @Override
    protected IndexingResponse compute() {
        long start = System.currentTimeMillis();
        Connection.Response response;
        try {
            List<IndexingThread> tasks = new ArrayList<>();
            response = Jsoup.connect(site.getUrl())
                    .timeout(3000)
                    .execute();
            Document doc = response.parse();
            Elements aTag = doc.getElementsByTag("a");
            int statusCode = response.statusCode();
            List<String> hrefs = new ArrayList<>();
            aTag.forEach(link -> hrefs.add(link.attr("href")));
            if (!checkIfContains(site.getUrl().substring(siteModel.getUrl().length()))) {
                saveToDB(doc, statusCode, site.getUrl());
            }
            iterateLinks(hrefs, tasks);
            tasks.forEach(ForkJoinTask::join);
            return ifSucceeded(start);
        } catch (IOException e) {
            siteRepository.changeStatusByUrl(site.getUrl(), Status.FAILED.name());
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void saveToDB(Document doc, int statusCode, String link) {
        PageModel page = buildPage(doc, statusCode, link);
        pageRepository.save(page);
        lemmaServiceImpl.buildLemmas(page.getContent(), page, siteModel);
        siteRepository.updateDateTime(siteModel.getId(), LocalDateTime.now());
    }

    private void iterateLinks(List<String> hrefs, List<IndexingThread> tasks) {
        for (String link : filterHrefs(hrefs)) {
            if (checkIfContains(link)) {
                continue;
            }
            indexPageAndLemmas(tasks, link);
        }
    }

    private List<String> filterHrefs(List<String> hrefs) {
        List<String> filtered = new ArrayList<>();
        for (String l : hrefs) {
            if (l.startsWith("/")) {
                filtered.add(l);
            }
        }
        return filtered;
    }

    private boolean checkIfContains(String link) {
        return pageRepository.getByPathAndId(link, siteModel.getId()).isPresent();
    }

    private String ifStartsWithSlash(String link) {
        return link.startsWith("/")  && link.length() > 1 ? siteModel.getUrl() + link : link;
    }

    private void indexPageAndLemmas(List<IndexingThread> tasks, String link) {
        Site site = new Site();
        site.setName(Thread.currentThread().getName());
        site.setUrl(ifStartsWithSlash(link));
        IndexingThread indexingThread = new IndexingThread(siteModel, site, pageRepository, siteRepository, lemmaServiceImpl);
        indexingThread.fork();
        tasks.add(indexingThread);
    }

    private PageModel buildPage(Document doc, int status, String path) {
        PageModel pageModel = new PageModel();
        pageModel.setCode(status);
        pageModel.setContent(doc.html());
        if (path.equals("/")) {
            pageModel.setPath(path);
        } else {
            pageModel.setPath(path.substring(siteModel.getUrl().length()));
        }
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
