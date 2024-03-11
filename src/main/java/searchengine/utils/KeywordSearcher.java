package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;

import java.util.*;


public class KeywordSearcher extends LemmaSearcher {
    private final ArrayList<Integer> priorityKeywords = new ArrayList<>();
    private final Set<Integer> keywords = new HashSet<>();
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
        Vector<String> contentWordsWithBold = null;
        for (String queryWord : queryWords) {
            contentWordsWithBold = getBoldedContent(queryWord, contentWordsWithBold, contentWords,
                    MorphService.getInstance().getMorphService(queryWords.get(0)));
        }
        if (contentWordsWithBold == null) {
            return contentWords;
        } else {
            return contentWordsWithBold;
        }
    }

    String getNormalWordForm(String contentWord, LuceneMorphology luceneMorphology) {
        String contentWordLowerCase = contentWord.toLowerCase(Locale.ROOT)
                .replaceAll("[^а-я]+", "").trim();
        if (!contentWordLowerCase.isBlank()) {
            List<String> wordBaseForms = luceneMorphology.getMorphInfo(contentWordLowerCase);
            if (!wordHasParticleProperty(wordBaseForms)) {
                List<String> normalForms = luceneMorphology.getNormalForms(contentWordLowerCase);
                if (!normalForms.isEmpty()) {
                    return normalForms.get(0);
                }
            }
        }
        return null;
    }

    public Integer getKeyword() {
        int result;
        Random random = new Random();
        if (!priorityKeywords.isEmpty()) {
            result = priorityKeywords.get(random.nextInt(priorityKeywords.size()));
        } else {
            if (keywords.isEmpty()) {
                result = 0;
            } else {
                result = new ArrayList<>(keywords).get(random.nextInt(keywords.size()));
            }
        }
        return result;
    }

    private Vector<String> setBoldWords(int wordIndex, String normalForm, String parentWord, Vector<String> contentWords) {
        for (String keyWord : contentWords) {
            if (keyWord.equals(parentWord)) {
                contentWords.set(contentWords.indexOf(keyWord), "<b>" + parentWord + "</b>");
            }
            if (normalForm.equals(mostRareLemma)) {
                priorityKeywords.add(wordIndex);
            } else {
                keywords.add(wordIndex);
            }
        }
        return contentWords;
    }

    private Vector<String> getBoldedContent(String queryWord, Vector<String> contentWordsWithBold, Vector<String> contentWords, LuceneMorphology luceneMorphology) {
        Vector<String> bolded = new Vector<>();
        String queryWordNormalForm = luceneMorphology.getNormalForms(queryWord).get(0);
        for (String contentWord : contentWords) {
            String normalFormContentWord = getNormalWordForm(contentWord, luceneMorphology);
            if (normalFormContentWord != null && normalFormContentWord.equals(queryWordNormalForm) && !bolded.contains(contentWord)) {
                contentWordsWithBold = setBoldWords(contentWords.indexOf(contentWord), normalFormContentWord, contentWord, contentWords);
                bolded.add(contentWord);
            }
        }
        return contentWordsWithBold;
    }
}
