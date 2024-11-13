package searchengine.services.search;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.DetailedSearchResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.lemma.LemmaProducer;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.*;

@Service
@AllArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaRepository lemmaRepository;

    private final PageRepository pageRepository;

    private final IndexRepository indexRepository;

    private final SiteRepository siteRepository;

    private final XMLParser xmlParser;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        if (query.isEmpty() || query.isBlank()) {
            return new SearchResponse(false, "Задан пустой поисковый запрос");
        }
        LemmaProducer lemmaProducer;
        try {
            lemmaProducer = LemmaProducer.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LinkedList<Lemma> sortedLemmas = buildLemmas(lemmaProducer.getLemmas(query).keySet());
        if (sortedLemmas.isEmpty()) {
            return new SearchResponse(false, "Страниц с заданным запросом не найдено");
        }
        LinkedHashMap<Lemma, List<Page>> pages = getFilteredPagesFromLemmas(sortedLemmas);
        if (pages.isEmpty()) {
            return new SearchResponse(false, "Страниц с заданным запросом не найдено");
        }
        LinkedHashMap<Lemma, List<Float>> ranks = fillEmpty(getRankings(pages));
        LinkedList<Float> relRanking = getRelativeR(getAbsRanking(ranks));
        return buildSearchResponse(aggrPagesAndRels(relRanking, pages.values().iterator().next()), query, limit);
    }

    private LinkedList<Lemma> buildLemmas(Set<String> queries) {
        if (queries.size() > 1) {
            int max = lemmaRepository.getHighestFrequency().get();
            return queries.stream()
                    .map(q -> lemmaRepository.getByLemma(q).orElse(new Lemma()))
                    .filter(q -> q.getFrequency() != null && q.getFrequency() < max * 0.9)
                    .sorted((o1, o2) -> {
                        int v1 = o1.getFrequency();
                        int v2 = o2.getFrequency();
                        return Integer.compare(v1, v2);
                    })
                    .collect(toCollection(LinkedList::new));
        } else {
            return queries.stream()
                    .map(q -> lemmaRepository.getByLemma(q).orElse(new Lemma()))
                    .collect(toCollection(LinkedList::new));
        }
    }

    private LinkedHashMap<Lemma, List<Page>> getFilteredPagesFromLemmas(List<Lemma> sortedLemmas) {
        List<List<Page>> list = sortedLemmas.stream()
                .map(l -> indexRepository.getAllPagesByLemmaId(l.getId()).orElse(new ArrayList<>()))
                .map(in -> in.stream()
                        .map(i -> pageRepository.getByPageId(i).orElse(new Page()))
                        .collect(toList()))
                .collect(toList());
        List<Page> first = list.get(0) != null ? list.get(0) : new ArrayList<>();
        LinkedHashMap<Lemma, List<Page>> filtered = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            list.get(i).retainAll(first);
            list.get(i).sort(Comparator.comparingInt(Page::getId));
            filtered.put(sortedLemmas.get(i), list.get(i));
            first = new ArrayList<>(list.get(i));
        }
        return filtered;
    }

    private LinkedHashMap<Lemma, List<Float>> getRankings(LinkedHashMap<Lemma, List<Page>> pages) {
        return pages.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().map(p ->
                                indexRepository.getRankByPageIdAndLemmaId(e.getKey().getId(), p.getId()).get())
                                .collect(toList()),
                        (oldV, newV) -> oldV,
                        LinkedHashMap::new
                ));
    }

    private LinkedHashMap<Lemma, List<Float>> fillEmpty(LinkedHashMap<Lemma, List<Float>> rankings) {
        int max = rankings.entrySet().iterator().next().getValue().size();
        for (Map.Entry<Lemma, List<Float>> entry : rankings.entrySet()) {
            if (entry.getValue().size() != max) {
                while(entry.getValue().size() != max) {
                    entry.getValue().add(0.0f);
                }
            }
        }
        return rankings;
    }

    private LinkedList<Float> getAbsRanking(LinkedHashMap<Lemma, List<Float>> ranks) {
        int size = ranks.values().iterator().next().size();
        return IntStream.range(0, size)
                .mapToObj(index -> ranks.values().stream()
                        .map(list -> list.get(index))
                        .reduce(Float::sum)
                        .orElse(0.0f))
                .collect(toCollection(LinkedList::new));
    }

    private LinkedList<Float> getRelativeR(LinkedList<Float> absRanking) {
        float max = Collections.max(absRanking);
        return absRanking.stream()
                .map(a -> a / max)
                .collect(toCollection(LinkedList::new));
    }

    private LinkedHashMap<Page, Float> aggrPagesAndRels(LinkedList<Float> rels, List<Page> pages) {
        return IntStream.range(0, pages.size())
                .boxed()
                .collect(toMap(
                        pages::get,
                        rels::get,
                        (oldV, newV) -> oldV,
                        LinkedHashMap::new
                ));
    }

    private SearchResponse buildSearchResponse(LinkedHashMap<Page, Float> rels, String query, int limit) {
        List<DetailedSearchResponse> list = new ArrayList<>();
        for (Map.Entry<Page, Float> entry : rels.entrySet()) {
            if (siteRepository.getSiteById(entry.getKey().getSite().getId()).isPresent()) {
                DetailedSearchResponse detailedSearchResponse = new DetailedSearchResponse();
                Site site = siteRepository.getSiteById(entry.getKey().getSite().getId()).get();
                detailedSearchResponse.setSiteUrl(site.getUrl());
                detailedSearchResponse.setSiteName(site.getName());
                detailedSearchResponse.setPageUri(entry.getKey().getPath().isEmpty() ? "/" : entry.getKey().getPath());
                detailedSearchResponse.setPageTitle(xmlParser.getTitle(entry.getKey().getContent()));
                detailedSearchResponse.setSnippet(getPageSnippet(entry.getKey(), query));
                detailedSearchResponse.setRelevance(entry.getValue());
                list.add(detailedSearchResponse);
            }
        }
        return limit < list.size() ? new SearchResponse(true, rels.size(), list.subList(0, limit))
                : new SearchResponse(true, rels.size(), list);
    }

    private String getPageSnippet(Page page, String query) {
        return xmlParser.parseContent(page.getContent(), query);
    }
}
