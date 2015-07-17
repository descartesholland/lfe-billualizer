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

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.modules.cPickle;

/**
 * @author Descartes
 *
 */
public class BillExplorer extends JPanel implements ActionListener, TreeSelectionListener, ListSelectionListener {
    static boolean debug = false;

    private static final long serialVersionUID = -2714378087612244399L;
    private static final String SOLR_URL = "http://localhost:8983/solr/";
    
    static JButton openButton;
    static JTextArea log;
    JFileChooser fc;
    static JList<String> stateList;
    static JTree directories;
    JEditorPane billsViewer;
    static File selectedStateJsonDir;
    JTextArea jsonViewer;
    JTabbedPane tabPane;
    static JButton searchButton;
    static JButton indexButton;
    
    static ButtonGroup searchTypeRadioGroup;
    static JRadioButton documentSearch;
    static JRadioButton assemblySearch;
    static JRadioButton stateSearch;
    static JRadioButton nationalSearch;
    
    static HashMap<String, ArrayList<String>> directoryToURL;
    static HashMap<String, ArrayList<String>> fileNameToURL;

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

        //Initialize file browser
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("state");
        directories = new JTree(root);
        directories.setPreferredSize(new Dimension(200, 300));
        directories.addTreeSelectionListener(this);

        JScrollPane directoriesScrollPane = new JScrollPane(directories);
        directoriesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        directoriesScrollPane.setPreferredSize(new Dimension(200, 300));

        //Left pane:
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, directoriesScrollPane, logScrollPane);
        leftSplitPane.setDividerLocation(350);
        logScrollPane.setMinimumSize(new Dimension(100, 100));

        //State list:
        stateList = new JList<String>(new String[1]);
        stateList.addListSelectionListener(this);
        stateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stateList.setLayoutOrientation(JList.VERTICAL_WRAP);
        stateList.setPreferredSize(new Dimension(200, 30));
        stateList.setMaximumSize(new Dimension(600, 30));
        stateList.setVisibleRowCount(1);
        JScrollPane stateListScrollPane = new JScrollPane(stateList);
        stateListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        stateListScrollPane.setPreferredSize(new Dimension(100, 30));
        stateListScrollPane.setMaximumSize(new Dimension(650, 30));

        //Set up tab pane:
        tabPane = new JTabbedPane();
        tabPane.setMinimumSize(new Dimension(200, 400));
        tabPane.setPreferredSize(new Dimension(650, 400));
        populateTabPane();
        //        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stateListScrollPane, tabPane);
        //        centerSplitPane.setDividerLocation(30);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
        centerPanel.add(stateListScrollPane);
        centerPanel.add(tabPane);

        JSplitPane masterSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplitPane, centerPanel);
        masterSplitPane.setDividerLocation(200);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(masterSplitPane, BorderLayout.LINE_START);  
    }

    /**
     * Removes all tabs from tabPane and generates each of them again
     * programmatically.
     */
    private void populateTabPane() {
        //Create JSON module:
        jsonViewer = new JTextArea(30, 40);
        jsonViewer.setPreferredSize(new Dimension(400, 300));
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

        //Create text module:
        JTextArea textViewer = new JTextArea(30, 40);
        textViewer.setPreferredSize(new Dimension(400, 300));
        textViewer.setMargin(new Insets(5, 5, 5, 5));
        textViewer.setEditable(false);
        JScrollPane textViewerScrollPane = new JScrollPane(textViewer);
        tabPane.addTab("Text", textViewerScrollPane);
        
        //Create search module:
        JPanel searchModule = new JPanel();
        searchModule.add(new JLabel("Search: "));
        
        JTextField searchBar = new JTextField(20);
        searchModule.add(searchBar);
        
        searchButton = new JButton("Search");
        searchButton.addActionListener(this);
        searchModule.add(searchButton);
        
        searchTypeRadioGroup = new ButtonGroup();
        documentSearch = new JRadioButton("Document");
        documentSearch.setSelected(true);
        assemblySearch = new JRadioButton("Assembly");
        stateSearch = new JRadioButton("State");
        nationalSearch = new JRadioButton("Nation");
        searchTypeRadioGroup.add(documentSearch);
        searchTypeRadioGroup.add(assemblySearch);
        searchTypeRadioGroup.add(stateSearch);
        searchTypeRadioGroup.add(nationalSearch);
        
        searchModule.add(documentSearch);
        searchModule.add(assemblySearch);
        searchModule.add(stateSearch);
        searchModule.add(nationalSearch);
        
        tabPane.addTab("Search", searchModule);
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
        //        frame.setPreferredSize(new Dimension(1000, 400));

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

                stateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
        else if(e.getSource() == searchButton) {
            
        }
        else if(e.getSource() == indexButton) {
            SolrInteractor interactor = new SolrInteractor(SOLR_URL);
        }
    }        

    public static HashMap<String, ArrayList<String>> loadPickle(File pickle) {
        HashMap<String, ArrayList<String>> pickleMap = new HashMap<String, ArrayList<String>>();
        log.append("Loading pickle " + pickle.getPath() + " of length " + pickle.length() + newline);
        BufferedReader bufR;
        StringBuilder strBuilder = new StringBuilder();
        try {
            bufR = new BufferedReader(new FileReader(pickle));
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
        if(arg0.getSource() == directories) {
            try {
                log.append("Looking for: " + ((FileTreeNode) directories.getSelectionPath().getLastPathComponent()).getTitle() + newline);
                billsViewer.setPage(fileNameToURL.get( ((FileTreeNode) directories.getSelectionPath().getLastPathComponent()).getTitle()).get(0));
            } catch (IOException e) {
                log.append("Invalid URL." + newline);
                if(debug) e.printStackTrace();
            } catch (NullPointerException e) {
                log.append("NPE. Check connectivity." + newline);
                if(debug) e.printStackTrace();
            }

            if(((FileTreeNode) arg0.getPath().getLastPathComponent()).isLeaf()) {
                try {
                    File json = ((FileTreeNode) arg0.getPath().getLastPathComponent()).getFile();
                    BufferedReader jsonReader = new BufferedReader(new FileReader(json));
                    StringBuilder jsonText = new StringBuilder();
                    String line;
                    while((line = jsonReader.readLine()) != null)
                        jsonText.append(line);
                    jsonReader.close();
                    jsonViewer.setText(jsonText.toString());
                } catch(FileNotFoundException e) {
                    log.append("File not found." + newline);
                    if(debug) e.printStackTrace();
                } catch(IOException e) {
                    log.append("Error reading file." + newline);
                    if(debug) e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        //Handle state switching:
        if(e.getSource() == stateList && e.getFirstIndex() != e.getLastIndex()) {
            log.append("Loading..." + newline);
            
            new DirectoriesSwingWorker().execute();
        }
    }
}
