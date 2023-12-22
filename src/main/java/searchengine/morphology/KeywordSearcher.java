package searchengine.morphology;

import org.apache.lucene.morphology.LuceneMorphology;
import java.util.*;

import static searchengine.morphology.MorphService.getMorphService;

public class KeywordSearcher extends LemmaSearcher {
    private final ArrayList<Integer> priorityKeywords = new ArrayList<>();
    private final ArrayList<Integer> keywords = new ArrayList<>();
    private static LuceneMorphology luceneMorphology = getMorphService();
    private final String content;
    private final String mostRareLemma;
    private final ArrayList<String> queryWords;

    public KeywordSearcher(String content, ArrayList<String> queryWords, String mostRareLemma) {
        this.content = content;
        this.queryWords = queryWords;
        this.mostRareLemma = mostRareLemma;
    }

    public Vector<String> getWordsForSnippets() {
        return getKeyWordsList(new Vector<>(Arrays.asList(content.split(" "))));
    }

    private Vector<String> getKeyWordsList(Vector<String> contentWords) {
        for (String contentWord : contentWords) {
            String word = contentWord.toLowerCase(Locale.ROOT)
                    .replaceAll("[^а-я]+", "").trim();
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
            String normalForm = normalForms.get(0);
            setBoldWords(contentWords.indexOf(word), normalForm, contentWord, contentWords);
        }
        return contentWords;
    }

    private void setBoldWords(int wordIndex, String normalForm, String parentWord, Vector<String> keyWords) {
        for (String keyWord : keyWords) {
            if (normalForm.equals(keyWord)) {
                keyWords.set(keyWords.indexOf(keyWord), "<b>" + parentWord + "</b>");
                if (normalForm.equals(mostRareLemma)) {
                    priorityKeywords.add(wordIndex);
                } else {
                    keywords.add(wordIndex);
                }
                break;
            }
        }
    }

    public Integer getKeyword() {
        int result;
        Random random = new Random();
        if (!priorityKeywords.isEmpty()) {
            result = priorityKeywords.get(random.nextInt(priorityKeywords.size()));
        } else {
            result = keywords.get(random.nextInt(keywords.size()));
        }
        return result;
    }
}
