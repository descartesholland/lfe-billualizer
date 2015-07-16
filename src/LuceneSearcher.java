import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;

public class LuceneSearcher {

    public static final String FILES_TO_INDEX_DIRECTORY = "filesToIndex";
    public static final String INDEX_DIRECTORY = "indexDirectory";

    public static final String FIELD_PATH = "path";
    public static final String FIELD_CONTENTS = "contents";

    public static void createIndex(File dirToIndex) throws CorruptIndexException, LockObtainFailedException, IOException {
        Analyzer analyzer = new StandardAnalyzer();
        Directory index = new RAMDirectory();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter indexWriter = new IndexWriter(index, config);

        File[] files = dirToIndex.listFiles();
        for (File file : files) {
            Document document = new Document();

            String path = file.getCanonicalPath();
            document.add(new StringField(FIELD_PATH, path, Field.Store.YES));

            Reader reader = new FileReader(file);
            document.add(new TextField(FIELD_CONTENTS, reader));

            indexWriter.addDocument(document);
        }
        indexWriter.close();
    }

/*    public static void searchIndex(String searchString) throws IOException, ParseException {
        System.out.println("Searching for '" + searchString + "'");
        Directory directory = FSDirectory.getDirectory(INDEX_DIRECTORY);
        IndexReader indexReader = IndexReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        Analyzer analyzer = new StandardAnalyzer();
        QueryParser queryParser = new QueryParser(FIELD_CONTENTS, analyzer);
        Query query = queryParser.parse(searchString);
        Hits hits = indexSearcher.search(query);
        System.out.println("Number of hits: " + hits.length());

        Iterator<Hit> it = hits.iterator();
        while (it.hasNext()) {
            Hit hit = it.next();
            Document document = hit.getDocument();
            String path = document.get(FIELD_PATH);
            System.out.println("Hit: " + path);
        }

    }*/

}