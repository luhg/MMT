package eu.modernmt.processing.chars;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.processing.TextProcessor;

import java.util.Map;

/**
 * Created by davide on 12/05/16.
 */
public class ControlCharsRemover extends TextProcessor<String, String> {

    public ControlCharsRemover(Language sourceLanguage, Language targetLanguage) throws UnsupportedLanguageException {
        super(sourceLanguage, targetLanguage);
    }

    public static String strip(String string) {
        char[] buffer = string.toCharArray();

        int newSize = 0;

        for (int i = 0; i < buffer.length; i++) {
            char c = buffer[i];

            if (c > 0x0008) {
                buffer[newSize] = c;
                newSize++;
            }
        }

        return new String(buffer, 0, newSize);
    }

    @Override
    public String call(String param, Map<String, Object> metadata) {
        return strip(param);
    }

}
