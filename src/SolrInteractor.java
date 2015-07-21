import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;


public class SolrInteractor {
    final static String newline = "\n";
    private String urlString;
    private SolrClient solr;
    private Tika tika;

    public SolrInteractor(String url) {
        urlString = url;
        solr = new HttpSolrClient(urlString, new DefaultHttpClient());
        tika = new Tika();
        tika.setMaxStringLength(1024*1024);
    }

    public String indexState(File stateDirectory) {
        List<String> failures = new ArrayList<String>();

        HashMap<String, String> urlToFileName = BillExplorer.loadPickleString(new File(new File(new File(BillExplorer.masterDir, BillExplorer.stateList.getSelectedValue()), "hashes"), "url_to_savedFileName_dict.p"));
        double counter = 0;
        File json = new File(new File(new File(stateDirectory, "json"), "bills"), stateDirectory.getName().toLowerCase());
        try {
            for(File assembly : json.listFiles()) {
                BillExplorer.log.append("Starting assembly " + assembly.getName() + newline);
                for(File house : assembly.listFiles()) {
                    counter = 0;
                    for(File f : house.listFiles()) {
                        counter++;
                        if(counter % 70 == 0) BillExplorer.log.append(house.getName() + " percentage complete: " + (counter / house.list().length) * 100 + newline);
                        SolrInputDocument doc = new SolrInputDocument();
                        doc.addField("state", stateDirectory.getName());
                        doc.addField("assembly", assembly.getName());
                        doc.addField("house", house.getName());
                        doc.addField("title", f.getName());

                        try {   
                            ArrayList<String> urls = BillExplorer.directoryToURL.get(f.getAbsolutePath().substring(stateDirectory.getAbsolutePath().length()+1).replace('\\', '/'));
                            String fileName = urlToFileName.get(urls.get(urls.size() - 1));

                            String content = tika.parseToString(TikaInputStream.get(new File(stateDirectory, fileName), new Metadata()));
                            content = content.replace("\n\n", "");
                            content = content.replace("  ", " ");
                            doc.addField("doctext_txt_en", content);
                        } catch (IOException e) {
                            failures.add(f.getAbsolutePath().substring(BillExplorer.masterDir.getAbsolutePath().length()));
                            BillExplorer.log.append("Error reading file " + f.getAbsolutePath() + newline);
                            if(BillExplorer.debug) e.printStackTrace();
                        } catch (TikaException e) {
                            failures.add(f.getAbsolutePath().substring(BillExplorer.masterDir.getAbsolutePath().length()));
                            BillExplorer.log.append("Tika Exception while parsing " + f.getAbsolutePath() + newline);
                            if(BillExplorer.debug) e.printStackTrace();
                        }

                        try {
                            UpdateResponse response = solr.add(doc);
                            if(response.getStatus() != 0 && response.getStatus() != 200) 
                                BillExplorer.log.append("Response status code: " + response.getStatus() + " header: " + response.getResponseHeader() + newline);
                        } catch(IOException | SolrServerException e) {
                            failures.add(f.getAbsolutePath().substring(BillExplorer.masterDir.getAbsolutePath().length()));
                            BillExplorer.log.append("Error occurred while adding document." + json.getAbsolutePath() + newline);
                            if(BillExplorer.debug) e.printStackTrace();
                        } catch(RemoteSolrException e) {
                            failures.add(f.getAbsolutePath().substring(BillExplorer.masterDir.getAbsolutePath().length()));
                            BillExplorer.log.append("Remote Exception adding document " + f.getName() + newline);
                            if(BillExplorer.debug) e.printStackTrace();
                        }
                    }
                }
                try {
                    BillExplorer.log.append("Committing to solr" + newline);
                    solr.commit();
                } catch (SolrServerException | IOException e) {
                    BillExplorer.log.append("Error occurred while commiting " + assembly.getPath() + " to server." + newline);
                    if(BillExplorer.debug) e.printStackTrace();
                }
            }
        } catch(NullPointerException e) {
            BillExplorer.log.append("Bad directory structure inside " + json.getAbsolutePath() + newline);
            if(BillExplorer.debug) e.printStackTrace();
        } 
        BillExplorer.log.append("Finished indexing state" + newline);
        System.out.println("Failures: " + failures);
        return "Success";
    }

    /**
     * Executes a search query on the Solr instance
     * @param text the text to search for
     * @param scope an array of name-value pairs containing the relevant filter information
     * and the desired scope of the query
     * @return A list of Files which match the query text within the scope
     * @throws IOException 
     * @throws SolrServerException 
     */
    public List<SolrDocument> query(String text, List<BasicNameValuePair> scope) throws SolrServerException, IOException {
        List<SolrDocument> ans = new ArrayList<SolrDocument>();
        SolrQuery query = new SolrQuery();
        query = query.setQuery(text);
        query = query.setFields("id", "title", "doctext_txt_en");
        for(BasicNameValuePair param : scope)
            query = query.addFilterQuery(param.getName() + ":" + "\"" + param.getValue() + "\"");
        query = query.setStart(0);
        query = query.setRows(Integer.MAX_VALUE);
        BillExplorer.log.append("Querying for: " + text + newline);

        QueryResponse response = solr.query(query);
        SolrDocumentList results = response.getResults();
        for(int i = 0; i < results.size(); i++) 
            ans.add(results.get(i));

        return ans;
    }
}
