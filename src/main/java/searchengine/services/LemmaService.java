package searchengine.services;

import searchengine.model.PageModel;
import searchengine.model.SiteModel;


public interface LemmaService {

    void buildLemmas(String content, PageModel pageModel, SiteModel site);
}
