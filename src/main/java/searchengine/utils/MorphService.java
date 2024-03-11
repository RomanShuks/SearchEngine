package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class MorphService {
    private static volatile MorphService instance;
    public static LuceneMorphology serviceRus;
    public static LuceneMorphology serviceEng;

    public static MorphService getInstance() {
        if (instance == null) {
            synchronized (MorphService.class) {
                if (instance == null) {
                    instance = new MorphService();
                }
            }
        }
        return instance;
    }

    private MorphService() {
        try {
            serviceRus = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            serviceEng = new EnglishLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LuceneMorphology getMorphService(String wordToCheck) {
        if (isCyrillic(wordToCheck)) {
            return serviceRus;
        } else {
            return serviceEng;
        }
    }

    public static boolean isCyrillic(String word) {
        char firstChar = word.toCharArray()[0];
        if (Character.isAlphabetic(firstChar)) {
            return Character.UnicodeBlock.of(firstChar).equals(Character.UnicodeBlock.CYRILLIC);
        } else {
            return Character.UnicodeBlock.of(firstChar).equals(Character.UnicodeBlock.BASIC_LATIN);
        }
    }

}
