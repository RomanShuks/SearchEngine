package searchengine.services;

import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.search.DetailedSearchItem;
import searchengine.dto.search.SearchResponse;
import searchengine.morphology.KeywordSearcher;
import searchengine.morphology.LemmaSearcher;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;

@Service
@Getter
public class SearchService {
    private List<DetailedSearchItem> detailed = new ArrayList<>();
    private HashMap<Integer, Double> relevance = new HashMap<>();
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    private final SearchResponse searchResponse = new SearchResponse();


    private int offset;
    private int limit;
    private int count;
    private int limitCounter;
    private String mostRareLemma;

    public SearchResponse getSearchResults(String query, String site, int offset, int limit) {
        this.limit = limit;
        this.offset = offset;
        count = 0;
        limitCounter = 0;
        ArrayList<String> words = new LemmaSearcher(query).getLemmasFromSearchQuery();
        searchResponse.setResult(true);
        searchResponse.setCount(0);
        searchResponse.setData(new ArrayList<>());

        if (words.isEmpty()) {
            return searchResponse;
        }
        return handleRequest(words, site);
    }

    private SearchResponse handleRequest(ArrayList<String> words, String site) {
        List<Site> sites = new ArrayList<>();
        if (site == null) {
            sites.addAll(siteRepository.findAll());
        } else {
            sites.add(siteRepository.findByUrl(site));
        }
        handleSites(sites, words);
        Map<Integer, Double> relevanceSorted = sortByValue(relevance);
        List<Integer> pageToResponse = new ArrayList<>(relevanceSorted.keySet());
        Collections.reverse(pageToResponse);
        collectSearchItems(pageToResponse, words);
        sortByRelevance(detailed);
        offset();
        return setDataInResponse();
    }

    private void handleSites(List<Site> sites, ArrayList<String> words) {
        for (int i = 0; i < sites.size(); i++) {
            HashMap<Lemma, Integer> lemmas = getLemmas(words, sites.get(i));
            if (lemmas.isEmpty()) {
                continue;
            }
            Map<Lemma, Integer> sortedLemmas = sortByValue(lemmas);
            List<Integer> pagesList = findPages(sortedLemmas);
            getRelevance(pagesList, sortedLemmas);
        }
    }

    private List<Integer> findPages(Map<Lemma, Integer> sortedLemmas) {
        List<Integer> indexesFirstPage = new ArrayList<>();
        Lemma firstLemma = sortedLemmas.keySet().iterator().next();
        mostRareLemma = firstLemma.getLemma();
        List<Index> indexes = indexRepository.findByLemma(firstLemma);

        for (Index index : indexes) {
            indexesFirstPage.add(index.getPage().getId());
        }
        sortedLemmas.remove(firstLemma);
        findMatchesWithRarePage(sortedLemmas, indexesFirstPage);

        for (int i = indexesFirstPage.size() - 1; i >= 0; i--) {
            if (indexesFirstPage.get(i) == 0) {
                indexesFirstPage.remove(i);
            }
        }
        sortedLemmas.put(firstLemma, 0);
        return indexesFirstPage;
    }

    public void collectSearchItems(List<Integer> pagesList, ArrayList<String> words) {
        ArrayList<String> titles = new ArrayList<>();
        if (pagesList.isEmpty()) {
            return;
        }
        for (int i = 0; i < pagesList.size(); i++) {
            if (limitCounter >= limit) {
                break;
            }
            int pageNumber = pagesList.get(i);
            DetailedSearchItem searchItem = new DetailedSearchItem();
            Page page = pageRepository.findById(pageNumber);
            searchItem.setSite(page.getSite().getUrl());
            searchItem.setSiteName(page.getSite().getName());
            searchItem.setUri(page.getPath());
            String title = getTitle(page.getPath());
            if (!titles.isEmpty() && titles.contains(title)) {
                continue;
            }
            searchItem.setTitle(title);
            titles.add(title);
            searchItem.setRelevance(relevance.get(pageNumber));
            searchItem.setSnippet(getSnippet(page.getContent(), words));
            detailed.add(searchItem);
            limitCounter++;
            count++;
        }
    }

    private void findMatchesWithRarePage(Map<Lemma, Integer> sortedLemmas, List<Integer> indexesFirstPage) {
        for(Map.Entry<Lemma, Integer> entry :sortedLemmas.entrySet()){
            ArrayList<Integer> indexesAnotherPage = new ArrayList<>();
            for(Index index : indexRepository.findByLemma(entry.getKey())){
                int pageNumber = index.getPage().getId();
                indexesAnotherPage.add(pageNumber);
            }

            for (int i = 0; i < indexesFirstPage.size(); i++) {
                boolean flag = false;
                int item = indexesFirstPage.get(i);
                for (int j = 0; j < indexesAnotherPage.size(); j++) {
                    if (item == indexesAnotherPage.get(j)) {
                        flag = true;
                    }
                }
                if (!flag) {
                    indexesFirstPage.set(i, 0);
                }
            }
        }
    }

    private void getRelevance(List<Integer> pagesList, Map<Lemma, Integer> sortedLemmas) {
        Set<Lemma> lemmas = sortedLemmas.keySet();
        for (int pageNumber : pagesList) {
            Page page = pageRepository.findById(pageNumber);
            double counter = 0;
            for (Lemma lemma : lemmas) {
                counter += indexRepository.findByPageAndLemma(page, lemma).getRank();
            }
            relevance.put(pageNumber, counter);
        }
    }

    private String getSnippet(String content, ArrayList<String> words) {
        KeywordSearcher keywordSearcher = new KeywordSearcher(Jsoup.parse(content).body().text(), words, mostRareLemma);
        Vector<String> snippetsWords = keywordSearcher.getWordsForSnippets();
        return String.valueOf(handlePhrase(snippetsWords, keywordSearcher.getKeyword()));
    }

    private StringBuilder handlePhrase(Vector<String> snippetsWords, int keyword) {
        Set<String> snippetsWordsSet = new HashSet<>(snippetsWords);
        StringBuilder stringBuilder = new StringBuilder();
        boolean hasFirst = false;
        for (String word : snippetsWordsSet) {
            while (stringBuilder.length() < 200) {
                if (word.matches("(\\<b\\>)*[а-я]+(\\<b\\>)*") && hasFirst) {
                    if(word.equals(snippetsWords.get(keyword))){
                        stringBuilder.append(word).append(" ");
                    }
                    word = word.replace("^\\<b\\>","");
                    word = word.replaceAll("\\</b\\>$","");
                    stringBuilder.append(word).append(" ");
                }
                if(word.matches("(\\<b\\>)*[а-я]+(\\<b\\>)*") && !hasFirst){
                    stringBuilder.append(word).append(" ");
                    hasFirst = true;
                }
                break;
            }
        }
        return stringBuilder;
    }

    private SearchResponse setDataInResponse() {
        searchResponse.setResult(true);
        if (detailed.size() <= limit) {
            searchResponse.setData(detailed);
            searchResponse.setCount(detailed.size());
        } else {
            searchResponse.setCount(limit);
            searchResponse.setData(detailed.subList(0, limit));
        }
        return searchResponse;
    }

    private String getTitle(String uri) {
        Document doc = null;
        try {
            doc = Jsoup.connect(uri).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc.title();
    }

    private void offset() {
        if (offset != 0) {
            for (int i = 0; i < offset; i++) {
                detailed.remove(i);
            }
        }
    }

    private HashMap<Lemma, Integer> getLemmas(ArrayList<String> words, Site site) {
        HashMap<Lemma, Integer> lemmas = new HashMap<>();
        for (String word : words) {
            Lemma lemma = lemmaRepository.findByLemmaAndSite(word, site);
            if (lemma == null) {
                break;
            }
            int wordFrequency = lemma.getFrequency();
            if (wordFrequency != 0) {
                lemmas.put(lemma, wordFrequency);
            }
        }
        return lemmas;
    }

    public void sortByRelevance(List<DetailedSearchItem> list) {
        list.sort(new Comparator<DetailedSearchItem>() {
            @Override
            public int compare(DetailedSearchItem o1, DetailedSearchItem o2) {
                if (o1.getRelevance() == o2.getRelevance()) return 0;
                else if (o1.getRelevance() < o2.getRelevance()) return 1;
                else return -1;
            }
        });
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

