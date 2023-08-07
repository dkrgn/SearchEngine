package searchengine.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import searchengine.model.IndexModel;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;

    private final PageRepository pageRepository;

    private final IndexRepository indexRepository;

    public void buildLemmas(String content, PageModel pageModel, SiteModel site) {
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
            indexBuilder(pageModel, site, lemmas);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void lemmaBuilder(String lemmaWord, SiteModel siteModel) {
        LemmaModel lemma = new LemmaModel();
        lemma.setLemma(lemmaWord);
        lemma.setFrequency(1);
        lemma.setSiteModel(siteModel);
        lemmaRepository.save(lemma);
    }

    private void indexBuilder(PageModel pageModel, SiteModel siteModel, HashMap<String, Integer> lemmas) {
        PageModel page = pageRepository.getByPathAndId(pageModel.getPath(), siteModel.getId()).get();
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            IndexModel index = new IndexModel();
            index.setRanking((float) entry.getValue());
            index.setLemmaModel(lemmaRepository.getByLemma(entry.getKey()).get());
            index.setPageModel(page);
            indexRepository.save(index);
        }
    }
}
