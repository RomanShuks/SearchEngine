package searchengine.morphology;

import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;

public class MorphService {

    static RussianLuceneMorphology getMorphService() {
        RussianLuceneMorphology service = null;
        try {
            service = new RussianLuceneMorphology();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return service;
    }
}
