package searchengine.services;

import searchengine.model.SiteModel;


public interface LemmaService {

    void buildLemmas(String doc, SiteModel site);
}
