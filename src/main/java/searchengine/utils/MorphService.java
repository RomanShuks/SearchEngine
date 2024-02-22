package searchengine.utils;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
public class MorphService {
    static LuceneMorphology selectMorphService(boolean isRussian) {
        LuceneMorphology service = null;
        if (isRussian) {
            try {
                service = new RussianLuceneMorphology();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                service = new EnglishLuceneMorphology();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return service;
    }

    public static LuceneMorphology getMorphService(String wordToCheck){
        if(isCyrillic(wordToCheck)){
             return selectMorphService(true);
        }else {
            return selectMorphService(false);
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
