package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.stereotype.Component;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.*;

import static searchengine.utils.MorphService.isCyrillic;

@Component
public class LemmaSearcher {
    private static LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "МС-П", " МС "};
    private static LemmaRepository lemmaRepository;
    private static IndexRepository indexRepository;
    private Page page;
    private String query;

    public LemmaSearcher() {
    }

    public LemmaSearcher(String query) {
        this.query = query;
    }

//    public LemmaSearcher(Page page, LemmaRepository lemmaRepository, IndexRepository indexRepository) {
//        this.page = page;
//        LemmaSearcher.lemmaRepository = lemmaRepository;
//        LemmaSearcher.indexRepository = indexRepository;
//    }

    public LemmaSearcher(LemmaRepository lemmaRepository, IndexRepository indexRepository) {
        LemmaSearcher.lemmaRepository = lemmaRepository;
        LemmaSearcher.indexRepository = indexRepository;
    }

    private ArrayList<String> getNormalWordsList() {
        Vector<String> words;
        ArrayList<String> lemmas = new ArrayList<>();
        if (page == null) {
            words = getWordsList(query);
        } else {
            words = getWordsList(page.getContent());
        }
        luceneMorphology = MorphService.getInstance().getMorphService(words.get(0));
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (wordHasParticleProperty(wordBaseForms)) {
                continue;
            }
            List<String> normalForms = luceneMorphology.getNormalForms(word);

            if (normalForms.isEmpty()) {
                continue;
            }
            lemmas.add(normalForms.get(0));
        }
        return lemmas;
    }

    public void putLemmasInBase(Page page){
        this.page = page;
        for (String normalWord : getNormalWordsList()) {
            Lemma lemma = lemmaRepository.findByLemmaAndSite(normalWord, page.getSite());
            Index index = indexRepository.findByPageAndLemma(page, lemma);
            collectNormalWord(normalWord, lemma, index);
        }
    }

    public ArrayList<String> getLemmasFromSearchQuery() {
        return getNormalWordsList();
    }

    private void putNewLemma(String normalWord) {
        Lemma lemma = new Lemma();
        lemma.setLemma(normalWord);
        lemma.setSite(page.getSite());
        lemma.setFrequency(1);
        lemmaRepository.save(lemma);
        putNewIndex(lemma);
    }

    private void putNewIndex(Lemma lemma) {
        Index index = new Index();
        index.setLemma(lemma);
        index.setPage(page);
        index.setRank(1F);
        indexRepository.save(index);
    }

    boolean wordHasParticleProperty(List<String> wordBaseForms) {
        return hasParticleProperty(wordBaseForms.get(0));
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }

    private Vector<String> getWordsList(String text) {
        Vector<String> result;
        if (isCyrillic(text)) {
            result = new Vector<>(Arrays.asList(text.toLowerCase(Locale.ROOT)
                    .replaceAll("[^а-я]+", " ")
                    .trim()
                    .split("\\s+")));

        } else {
            result = new Vector<>(Arrays.asList(text.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z]+", " ")
                    .trim()
                    .split("\\s+")));
        }
        return result;
    }

    private void collectNormalWord(String normalWord, Lemma lemma, Index index) {
        if (lemma == null) {
            putNewLemma(normalWord);
        } else {
            if (index == null) {
                putNewIndex(lemma);
                int frequency = lemma.getFrequency();
                lemma.setFrequency(frequency + 1);
                lemmaRepository.save(lemma);
            } else {
                float rank = index.getRank();
                index.setRank(rank + 1);
                indexRepository.save(index);
            }
        }
    }
}
