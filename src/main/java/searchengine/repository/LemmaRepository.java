package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.ArrayList;

public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findByLemmaAndSite(String lemma, Site site);
    ArrayList<Lemma> findByLemma(String lemma);
    int countBySite(Site site);
}
