package searchengine.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;

    private final PageRepository pageRepository;

    private final IndexRepository indexRepository;

    public void buildLemmas(String content, Page page, Site site) {
        try {
            LemmaProducer lemmaProducer = LemmaProducer.getInstance();
            HashMap<String, Integer> lemmas = lemmaProducer.getLemmas(content);
            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                if (lemmaRepository.getByLemma(entry.getKey()).isPresent()) {
                    lemmaRepository.updateFrequency(entry.getKey());
                    continue;
                }
                lemmaBuilder(entry.getKey(), site);
            }
            indexBuilder(page, site, lemmas);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void lemmaBuilder(String lemmaWord, Site site) {
        Lemma lemma = new Lemma();
        lemma.setLemma(lemmaWord);
        lemma.setFrequency(1);
        lemma.setSite(site);
        lemmaRepository.save(lemma);
    }

    private void indexBuilder(Page pageModel, Site site, HashMap<String, Integer> lemmas) {
        Page page = pageRepository.getByPathAndId(pageModel.getPath(), site.getId()).get();
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            Index index = new Index();
            index.setRanking((float) entry.getValue());
            index.setLemma(lemmaRepository.getByLemma(entry.getKey()).get());
            index.setPage(page);
            indexRepository.save(index);
        }
    }
}
