import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;


/**
 * @author Descartes
 *
 */
public class DirectoriesSwingWorker extends SwingWorker<MyTreeModel, Object> {

    @Override
    protected MyTreeModel doInBackground() throws Exception {
      //Perform some computation/loading:
        BillExplorer.directoryToURL = BillExplorer.loadPickle(new File(new File(new File(BillExplorer.masterDir, BillExplorer.stateList.getSelectedValue()), "hashes"), "json_to_url_dict.p"));
        BillExplorer.fileNameToURL = new HashMap<String, ArrayList<String>>();
        for(String key : BillExplorer.directoryToURL.keySet()) {
            try {
                BillExplorer.fileNameToURL.put(key.substring(key.lastIndexOf('/')+1), BillExplorer.directoryToURL.get(key));
            }
            catch(IndexOutOfBoundsException e2) {
                BillExplorer.log.append("Non-json file \"" + key + "\" detected" + BillExplorer.newline);
                if(BillExplorer.debug) e2.printStackTrace();
            }
        }

        BillExplorer.selectedStateJsonDir = new File(new File(new File(new File(BillExplorer.masterDir, BillExplorer.stateList.getSelectedValue()), "json"), "bills"), BillExplorer.stateList.getSelectedValue().toLowerCase());
        return new MyTreeModel(new FileTreeNode(BillExplorer.selectedStateJsonDir, BillExplorer.stateList.getSelectedValue()));

    }
    
    @Override
    protected void done() {
        try {
            BillExplorer.directories.setModel(get());
            BillExplorer.log.append("Finished loading state." + BillExplorer.newline);
        } catch (InterruptedException e) {
            BillExplorer.log.append("Interrupted." + BillExplorer.newline);
            if(BillExplorer.debug) e.printStackTrace();
        } catch (ExecutionException e) {
            BillExplorer.log.append("Execution exception" + BillExplorer.newline);
            if(BillExplorer.debug) e.printStackTrace();
        }
    }

}
