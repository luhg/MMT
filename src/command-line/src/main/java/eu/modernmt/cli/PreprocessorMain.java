package eu.modernmt.cli;

import eu.modernmt.io.*;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by davide on 17/12/15.
 */
public class PreprocessorMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option sourceLanguage = Option.builder("s").hasArg().required().build();
            Option targetLanguage = Option.builder("t").hasArg().required().build();
            Option skipTags = Option.builder().longOpt("no-tags").hasArg(false).required(false).build();
            Option skipPlaceholders = Option.builder().longOpt("print-placeholders").hasArg(false).required(false).build();
            Option keepSpaces = Option.builder().longOpt("original-spacing").hasArg(false).required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(sourceLanguage);
            cliOptions.addOption(targetLanguage);
            cliOptions.addOption(skipTags);
            cliOptions.addOption(skipPlaceholders);
            cliOptions.addOption(keepSpaces);
        }

        public final LanguagePair language;
        public final boolean printTags;
        public final boolean printPlaceholders;
        public final boolean keepSpaces;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language source = Language.fromString(cli.getOptionValue("s"));
            Language target = Language.fromString(cli.getOptionValue("t"));
            language = new LanguagePair(source, target);
            printTags = !cli.hasOption("no-tags");
            printPlaceholders = cli.hasOption("print-placeholders");
            keepSpaces = cli.hasOption("original-spacing");
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        Preprocessor preprocessor = null;
        Outputter output = null;

        LineReader input = new UnixLineReader(System.in, DefaultCharset.get());

        try {
            preprocessor = new Preprocessor();

            if (args.keepSpaces)
                output = new SentenceOutputter(args.printTags, args.printPlaceholders);
            else
                output = new TokensOutputter(args.printTags, args.printPlaceholders);

            String line;
            while ((line = input.readLine()) != null) {
                Sentence sentence = preprocessor.process(args.language, line);
                output.write(sentence);
            }
        } finally {
            IOUtils.closeQuietly(preprocessor);
            IOUtils.closeQuietly(output);
        }
    }

    private interface Outputter extends Closeable {

        void write(Sentence value) throws IOException;

    }

    private static class SentenceOutputter implements Outputter {

        private final UnixLineWriter writer;
        private final boolean printTags;
        private final boolean printPlaceholders;

        public SentenceOutputter(boolean printTags, boolean printPlaceholders) {
            this.writer = new UnixLineWriter(System.out, DefaultCharset.get());
            this.printTags = printTags;
            this.printPlaceholders = printPlaceholders;
        }

        @Override
        public void write(Sentence value) throws IOException {
            writer.writeLine(value.toString(printTags, printPlaceholders));
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }

    private static class TokensOutputter implements Outputter {

        private final TokensOutputStream stream;

        public TokensOutputter(boolean printTags, boolean printPlaceholders) {
            stream = new TokensOutputStream(System.out, printTags, printPlaceholders);
        }

        @Override
        public void write(Sentence value) throws IOException {
            stream.write(value);
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }

}
