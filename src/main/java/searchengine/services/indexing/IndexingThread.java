package searchengine.services.indexing;

import lombok.AllArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemma.LemmaServiceImpl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public class IndexingThread extends RecursiveAction {

    private final Site site;

    private final String passedUrl;

    private final PageRepository pageRepository;

    private final SiteRepository siteRepository;

    private final LemmaServiceImpl lemmaServiceImpl;

    private static final int THRESHOLD = 2000;

    @Override
    protected void compute() {
        long start = System.currentTimeMillis();
        Connection.Response response;
        try {
            List<IndexingThread> tasks = new ArrayList<>();
            response = Jsoup.connect(passedUrl)
                    .timeout(3000)
                    .execute();
            Document doc = response.parse();
            Elements aTag = doc.getElementsByTag("a");
            int statusCode = response.statusCode();
            List<String> hrefs = new ArrayList<>();
            aTag.forEach(url -> hrefs.add(url.attr("href")));
            if (!checkIfContains(passedUrl.substring(site.getUrl().length()))) {
                saveToDB(doc, statusCode, passedUrl);
            }
            iterateUrls(hrefs, tasks);
            if (pageRepository.getAllBySiteId(site.getId()).get().size() >= THRESHOLD) {
                siteRepository.changeStatusByUrl(site.getUrl(), Status.INDEXED.name());
            }
        } catch (IOException e) {
            siteRepository.changeStatusByUrl(passedUrl, Status.FAILED.name());
            siteRepository.setLastError(e.getMessage(), site.getId());
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void saveToDB(Document doc, int statusCode, String url) {
        Page page = buildPage(doc, statusCode, url);
        pageRepository.save(page);
        lemmaServiceImpl.buildLemmas(page.getContent(), page, site);
        siteRepository.updateDateTime(site.getId(), LocalDateTime.now());
    }

    private void iterateUrls(List<String> hrefs, List<IndexingThread> tasks) {
        for (String url : filterHrefs(hrefs)) {
            if (checkIfContains(url)) {
                continue;
            }
            startNewThread(tasks, url);
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

    private boolean checkIfContains(String url) {
        return pageRepository.getByPathAndId(url, site.getId()).isPresent();
    }

    private String ifStartsWithSlash(String url) {
        return url.startsWith("/")  && url.length() > 1 ? site.getUrl() + url : url;
    }

    private void startNewThread(List<IndexingThread> tasks, String url) {
        IndexingThread indexingThread = new IndexingThread(site, ifStartsWithSlash(url), pageRepository, siteRepository, lemmaServiceImpl);
        indexingThread.fork();
        tasks.add(indexingThread);
    }

    private Page buildPage(Document doc, int status, String path) {
        Page page = new Page();
        page.setCode(status);
        page.setContent(doc.html());
        if (path.equals("/")) {
            page.setPath(path);
        } else {
            page.setPath(path.substring(site.getUrl().length()));
        }
        page.setSite(site);
        return page;
    }

//    private IndexingResponse ifSucceeded(long start) {
//        if (pageRepository.getAll().isPresent()) {
//            System.err.println("EXECUTED: " + (System.currentTimeMillis() - start));
//            siteRepository.changeStatusByUrl(site.getUrl(), Status.INDEXED.name());
//            return new IndexingResponse(true);
//        }
//        siteRepository.changeStatusByUrl(passedUrl, Status.FAILED.name());
//        siteRepository.setLastError("Ошибка индексации", site.getId());
//        return new IndexingResponse(false, "Ошибка индексации");
//    }
}
