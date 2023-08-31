package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class LemmaFinder {

    public static LuceneMorphology luceneMorph;
    static {
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final String wordRegex = "\\s*[^а-яА-Я]\\s*";
    private static final String unnecessaryPartsOfSpeech = "[а-яА-Я]+\\|[nolp]\\s[ПРЕДЛМЖСОЮЗЧАТ]+";


    public HashMap<String, Integer> getLemmasCollection(String text){

        HashMap<String, Integer> lemmas = new HashMap<>();
        if(text.isEmpty()) {
            return lemmas;
        }

        List<String> words = divisionOfTextIntoWords(removeTagsFromText(text));
        for (String word : words) {
            if (word.isBlank() || !isCorrectWordForm(word)) {
                continue;
            }

            List<String> normalForms = luceneMorph.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);
            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
            } else {
                lemmas.put(normalWord, 1);
            }
        }
        return lemmas;
    }


    private List<String> divisionOfTextIntoWords(String text) {
        return List.of(text.toLowerCase().split(wordRegex));
    }


    private boolean isCorrectWordForm(String word) {
        List<String> wordInfo = luceneMorph.getMorphInfo(word);
        for (String morphInfo : wordInfo) {
            if (morphInfo.matches(unnecessaryPartsOfSpeech)) {
                return false;
            }
        }
        return true;
    }


    private String removeTagsFromText(String html) {
        return Jsoup.parse(html).text();
    }

}
