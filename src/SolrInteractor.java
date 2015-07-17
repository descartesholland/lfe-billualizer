import java.io.File;
import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;


public class SolrInteractor {
    private String urlString = "http://localhost:8983/solr/";
    private SolrClient solr;
    
    public SolrInteractor(String url) {
        urlString = url;
        solr = new HttpSolrClient(urlString);
    }

    public String indexState(File stateDirectory) {
        File json = new File(new File(new File(stateDirectory, "json"), "bills"), stateDirectory.getName().toLowerCase());
        try {
            for(File assembly : json.listFiles()) {
                for(File house : assembly.listFiles()) {
                    for(File f : house.listFiles()) {
                        SolrInputDocument doc = new SolrInputDocument();
                        doc.addField("assembly", assembly.getName());
                        doc.addField("house", house.getName());
                        doc.addField("title", f.getName());
                        Parser p = new AutoDetectParser();
                        BodyContentHandler handler = new BodyContentHandler( );
                        Metadata metadata = new Metadata();
                        ParseContext context = new ParseContext();
                        try {
                            p.parse(TikaInputStream.get(f), handler, metadata, context);
                        } catch (IOException e) {
                            BillExplorer.log.append("Error reading file " + f.getAbsolutePath());
                            if(BillExplorer.debug) e.printStackTrace();
                        } catch (SAXException e) {
                            BillExplorer.log.append("SAX Exception while parsing " + f.getAbsolutePath());
                            if(BillExplorer.debug) e.printStackTrace();
                        } catch (TikaException e) {
                            BillExplorer.log.append("Tika Exception while parsing " + f.getAbsolutePath());
                            if(BillExplorer.debug) e.printStackTrace();
                        }
                        doc.addField("text", handler.toString());

                        try {
                            UpdateResponse response = solr.add(doc);
                            if(response.getStatus() != 200)
                                System.out.println(response.getStatus());
                        } catch(IOException | SolrServerException e) {
                            BillExplorer.log.append("Error occurred while adding document." + json.getAbsolutePath());
                            if(BillExplorer.debug) e.printStackTrace();
                        }
                    }
                }
                try {
                    solr.commit();
                } catch (SolrServerException | IOException e) {
                    BillExplorer.log.append("Error occurred while commiting " + assembly.getPath() + " to server.");
                    if(BillExplorer.debug) e.printStackTrace();
                }
            } 
        } catch(NullPointerException e) {
            BillExplorer.log.append("Bad directory structure inside " + json.getAbsolutePath());
            if(BillExplorer.debug) e.printStackTrace();
        } 
        return "Success";
    }
}
