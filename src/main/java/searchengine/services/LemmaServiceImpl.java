package searchengine.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaModel;
import searchengine.model.SiteModel;
import searchengine.repositories.LemmaRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;

    public void buildLemmas(String doc, SiteModel site) {
        try {
            LemmaProducer lemmaProducer = LemmaProducer.getInstance();
            HashMap<String, Integer> lemmas = lemmaProducer.getLemmas(doc);
            for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
                if (lemmaRepository.getByLemma(entry.getKey()).isPresent()) {
                    lemmaRepository.updateFrequency(entry.getKey());
                    continue;
                }
                LemmaModel lemma = new LemmaModel();
                lemma.setLemma(entry.getKey());
                lemma.setFrequency(1);
                lemma.setSiteModel(site);
                lemmaRepository.save(lemma);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
