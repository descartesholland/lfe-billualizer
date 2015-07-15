import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.modules.cPickle;

/**
 * @author Descartes
 *
 */
public class BillExplorer extends JPanel implements ActionListener, TreeSelectionListener {
    boolean debug = false;

    private static final long serialVersionUID = -2714378087612244399L;
    JButton openButton;
    JTextArea log;
    JFileChooser fc;
    JList<String> stateList;
    JTree directories;
    JEditorPane billsViewer;
    File selectedStateJsonDir;

    HashMap<String, ArrayList<String>> directoryToURL;
    HashMap<String, ArrayList<String>> fileNameToURL;

    final static String newline = "\n";

    static File masterDir;

    public BillExplorer() {
        super(new BorderLayout());

        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

        //Create a file chooser
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        openButton = new JButton("Choose Root Directory...");
        openButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);

        stateList = new JList<String>(new String[7]);
        JScrollPane stateListScrollPane = new JScrollPane(stateList);

        //Left pane:
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stateListScrollPane, logScrollPane);
        leftSplitPane.setDividerLocation(350);
        logScrollPane.setMinimumSize(new Dimension(100, 100));
        stateListScrollPane.setMinimumSize(new Dimension(100, 200));

        //Bill explorer:
        directoryToURL = loadPickle("json_to_url_dict.p");
        fileNameToURL = new HashMap<String, ArrayList<String>>();
        for(String key : directoryToURL.keySet()) {
            try {
                fileNameToURL.put(key.substring(key.lastIndexOf('/')+1), directoryToURL.get(key));
            }
            catch(IndexOutOfBoundsException e) {
                log.append("Non-json file \"" + key + "\" detected" + newline);
                if(debug) e.printStackTrace();
            }
        }
        String[] billNames = new String[fileNameToURL.keySet().size()];
        fileNameToURL.keySet().toArray(billNames);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("state");
        directories = new JTree(root);
        directories.setPreferredSize(new Dimension(200, 500));
        directories.addTreeSelectionListener(this);

        JScrollPane directoriesScrollPane = new JScrollPane(directories);
        directoriesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        directoriesScrollPane.setPreferredSize(new Dimension(200, 500));

        //Main pane tabs:
        JTabbedPane tabPane = new JTabbedPane();
        tabPane.setMinimumSize(new Dimension(200, 400));
        tabPane.setPreferredSize(new Dimension(800, 500));

        JTextArea jsonViewer = new JTextArea(30, 40);
        jsonViewer.setPreferredSize(new Dimension(800, 300));
        jsonViewer.setMargin(new Insets(5, 5, 5, 5));
        jsonViewer.setEditable(false);
        JScrollPane jsonViewerScrollPane = new JScrollPane(jsonViewer);
        //        jsonViewerScrollPane.setPreferredSize(new Dimension(200, 200));
        tabPane.addTab("JSON", jsonViewerScrollPane);

        //Create bill viewer module:
        billsViewer = new JEditorPane();
        billsViewer.setPreferredSize(new Dimension(400, 300));
        billsViewer.setMargin(new Insets(5, 5, 5, 5));
        billsViewer.setEditable(false);

        JScrollPane billsViewerScrollPane = new JScrollPane(billsViewer);

        tabPane.addTab("Bills", billsViewerScrollPane);

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, directoriesScrollPane, tabPane);
        centerSplitPane.setDividerLocation(150);
        JSplitPane masterSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, centerSplitPane);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(masterSplitPane, BorderLayout.LINE_START);  
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE); 
                createAndShowGUI();
            }
        });
    }

    
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Bill Explorer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        frame.add(new BillExplorer());

        //Display the window.
        frame.setPreferredSize(new Dimension(1000, 600));

        frame.pack();
        frame.setVisible(true);
    }

    
    @Override
    public void actionPerformed(ActionEvent e) {
        //Handle open button action.
        if(e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(BillExplorer.this);

            if(returnVal == JFileChooser.APPROVE_OPTION) {
                masterDir = fc.getSelectedFile();
                log.append("Opening: " + masterDir.getName() + "." + newline);
                
                //Populate states list:
                DefaultListModel<String> model = new DefaultListModel<String>();
                for(String fileName : masterDir.list())
                    model.addElement(fileName);
                stateList.setModel(model);

                ListSelectionModel listSel = new DefaultListSelectionModel();
                listSel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                stateList.setSelectionModel(listSel);
                stateList.setSelectedIndex(0);
                selectedStateJsonDir = new File(new File(new File(new File(masterDir, stateList.getSelectedValue()), "json"), "bills"), stateList.getSelectedValue().toLowerCase());
                
                //Populate directories for default state:
                directories.setModel(new MyTreeModel(new FileTreeNode(selectedStateJsonDir, stateList.getSelectedValue())));
            } 
            else {
                log.append("Open command cancelled by user." + newline);
                log.setCaretPosition(log.getDocument().getLength());
            }
        }
    }        

    public HashMap<String, ArrayList<String>> loadPickle(String pickle) {
        HashMap<String, ArrayList<String>> pickleMap = new HashMap<String, ArrayList<String>>();
        File f = new File(System.getProperty("user.dir") + "//assets//" + pickle);
        log.append("Loading pickle of length " + f.length() + newline);
        BufferedReader bufR;
        StringBuilder strBuilder = new StringBuilder();
        try {
            bufR = new BufferedReader(new FileReader(f));
            String aLine;
            while (null != (aLine = bufR.readLine())) {
                strBuilder.append(aLine).append(newline);
            }
            bufR.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PyString pyStr = new PyString(strBuilder.toString());
        PyDictionary idToCountriesObj =  (PyDictionary) cPickle.loads(pyStr);
        ConcurrentMap<PyObject, PyObject> aMap = idToCountriesObj.getMap();
        for (Map.Entry<PyObject, PyObject> entry : aMap.entrySet()) {
            String appId = entry.getKey().toString();
            PyList countryIdList = (PyList) entry.getValue();
            List<String> countryList = (List<String>) countryIdList.subList(0, countryIdList.size());
            ArrayList<String> countryArrList = new ArrayList<String>(countryList);
            pickleMap.put(appId, countryArrList);
        }
        return pickleMap;
    }

    @Override
    public void valueChanged(TreeSelectionEvent arg0) {
//        if(arg0.getSource() == directories) {
//            try {
//                billsViewer.setPage(fileNameToURL.get( ((FileTreeNode) directories.getSelectionPath().getLastPathComponent()).getTitle()).get(0));
//            } catch (IOException e) {
//                log.append("Invalid URL." + newline);
//                if(debug) e.printStackTrace();
//            }
//        }
    }
}
