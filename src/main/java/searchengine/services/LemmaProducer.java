package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LemmaProducer {

    private static LuceneMorphology morphology;

    private final List<String> nonUsableWords = Arrays.asList("СОЮЗ", "ПРЕДЛ", "МЕЖД", "ЧАСТ");

    public static LemmaProducer getInstance() throws IOException {
        morphology = new RussianLuceneMorphology();
        return new LemmaProducer();
    }

    public HashMap<String, Integer> getLemmas(String words) {
        return buildLemmas(words);
    }

    private HashMap<String, Integer> buildLemmas(String words) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String s : filteredForNonUsableWords(words)) {
            if (lemmas.containsKey(s)) {
                lemmas.put(s, lemmas.get(s) + 1);
            } else {
                lemmas.put(s, 1);
            }
        }
        return lemmas;
    }

    private List<String> filteredForNonUsableWords(String words) {
        List<String> formatted = new ArrayList<>();
        List<String> usableWords = new ArrayList<>();
        for (String s : filteredAndLowerCasedWords(words)) {
            formatted.add(morphology.getNormalForms(s).get(0));
        }
        formatted.forEach(f -> morphology.getMorphInfo(f).forEach(w -> {
            if (nonUsableWords.stream().noneMatch(w::contains)) {
                usableWords.add(f);
            }
        }));
        return usableWords;
    }

    private List<String> filteredAndLowerCasedWords(String text) {
        List<String> words = Arrays.stream(
                text
                        .replaceAll("[^а-яА-Я\\s]", " ")
                        .trim()
                        .split("[\\s.,!?\\t\\n]"))
                        .filter(l -> !l.isEmpty())
                        .collect(Collectors.toList()
                );
        List<String> lowered = new ArrayList<>();
        words.forEach(w -> lowered.add(w.toLowerCase()));
        return lowered;
    }
}
