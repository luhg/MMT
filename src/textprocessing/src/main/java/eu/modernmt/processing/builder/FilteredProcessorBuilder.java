package eu.modernmt.processing.builder;

import eu.modernmt.lang.Language;

import java.util.Collections;
import java.util.HashSet;

/**
 * Created by davide on 31/05/16.
 */
class FilteredProcessorBuilder extends ProcessorBuilder {

    private final Filter sourceFilter;
    private final Filter targetFilter;

    public FilteredProcessorBuilder(String className, String sourceFilter, String targetFilter) {
        super(className);
        this.sourceFilter = parseFilter(sourceFilter);
        this.targetFilter = parseFilter(targetFilter);
    }

    private static Filter parseFilter(String definition) {
        if (definition == null)
            return null;

        definition = definition.trim();
        if (definition.length() < 2)
            return null;

        if (definition.charAt(0) == '^')
            return new NorFilter(definition.substring(1).split("\\s+"));
        else
            return new OrFilter(definition.split("\\s+"));
    }

    public boolean accept(Language sourceLanguage, Language targetLanguage) {
        if (this.sourceFilter != null && !this.sourceFilter.accept(sourceLanguage))
            return false;
        if (this.targetFilter != null && !this.targetFilter.accept(targetLanguage))
            return false;
        return true;
    }

    private interface Filter {

        boolean accept(Language language);

    }

    private static class OrFilter implements Filter {

        protected final HashSet<String> langs;

        private OrFilter(String[] langs) {
            this.langs = new HashSet<>();
            Collections.addAll(this.langs, langs);
        }

        @Override
        public boolean accept(Language language) {
            if (language == null)
                return false;
            String tag = language.toLanguageTag();
            return langs.contains(tag);
        }
    }

    private static class NorFilter extends OrFilter {

        private NorFilter(String[] langs) {
            super(langs);
        }

        @Override
        public boolean accept(Language language) {
            return !super.accept(language);
        }
    }
}
