package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.analysis.ContextAnalyzerIndex;
import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.context.lucene.storage.Options;
import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.StringCorpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 09/05/16.
 */
public class LuceneAnalyzer implements ContextAnalyzer {

    private final Logger logger = LogManager.getLogger(LuceneAnalyzer.class);

    private final ContextAnalyzerIndex index;
    private final CorporaStorage storage;

    public LuceneAnalyzer(LanguageIndex languages, File indexPath) throws IOException {
        this(languages, indexPath, new Options());
    }

    public LuceneAnalyzer(LanguageIndex languages, File indexPath, Options options) throws IOException {
        this.index = new ContextAnalyzerIndex(new File(indexPath, "index"));
        this.storage = new CorporaStorage(new File(indexPath, "storage"), options, this.index, languages);
    }

    public LuceneAnalyzer(ContextAnalyzerIndex index, CorporaStorage storage) {
        this.index = index;
        this.storage = storage;
    }

    public ContextAnalyzerIndex getIndex() {
        return index;
    }

    public CorporaStorage getStorage() {
        return storage;
    }

    @Override
    public void add(Memory memory, MultilingualCorpus corpus) throws ContextAnalyzerException {
        HashMap<Memory, MultilingualCorpus> map = new HashMap<>(1);
        map.put(memory, corpus);

        this.add(map);
    }

    @Override
    public void add(Map<Memory, MultilingualCorpus> corpora) throws ContextAnalyzerException {
        for (Map.Entry<Memory, MultilingualCorpus> entry : corpora.entrySet()) {
            long id = entry.getKey().getId();

            try {
                this.storage.bulkInsert(id, entry.getValue());
            } catch (IOException e) {
                throw new ContextAnalyzerException("Unable to add memory " + id, e);
            }
        }

        try {
            this.storage.flushToDisk(false, true);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to write memory index to disk", e);
        }
    }

    @Override
    public ContextVector getContextVector(LanguagePair direction, String query, int limit) throws ContextAnalyzerException {
        return getContextVector(direction, new StringCorpus(null, direction.source, query), limit);
    }

    @Override
    public ContextVector getContextVector(LanguagePair direction, File source, int limit) throws ContextAnalyzerException {
        return getContextVector(direction, new FileCorpus(source, null, direction.source), limit);
    }

    @Override
    public ContextVector getContextVector(LanguagePair direction, Corpus query, int limit) throws ContextAnalyzerException {
        try {
            return this.index.getContextVector(direction, query, limit);
        } catch (IOException e) {
            throw new ContextAnalyzerException("Failed to calculate context-vector due an internal error", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.storage.shutdown();
            this.storage.awaitTermination(TimeUnit.SECONDS, 2);
        } catch (InterruptedException e) {
            // Ignore it
        } finally {
            this.index.close();
        }
    }

    // UpdateListener

    @Override
    public void onDataReceived(DataBatch batch) throws ContextAnalyzerException {
        Collection<Deletion> deletions;

        try {
            deletions = storage.onDataReceived(batch);
        } catch (IOException e) {
            throw new ContextAnalyzerException(e);
        }

        if (deletions.isEmpty()) {
            // Skip disk flush if no deletions
            return;
        }

        boolean indexUpdated = false;
        boolean storageUpdated = false;

        try {
            storage.flushToDisk(true, false);
            storageUpdated = true;
        } catch (IOException e) {
            logger.error("Storage flush failed ", e);
        }

        try {
            for (Deletion deletion : deletions) {
                index.delete(deletion.memory);
                logger.info("Memory deleted from ContextAnalyzer index: " + deletion.memory);
            }

            index.flush();

            indexUpdated = true;
        } catch (IOException e) {
            logger.error("Index flush failed", e);
        }

        if (!indexUpdated || !storageUpdated)
            throw new ContextAnalyzerException("Failed update LuceneAnalyzer");
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return storage.getLatestChannelPositions();
    }

}
