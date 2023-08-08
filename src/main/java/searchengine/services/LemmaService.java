package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;


public interface LemmaService {

    void buildLemmas(String content, Page page, Site site);
}
