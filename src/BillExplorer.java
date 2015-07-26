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
import javax.swing.GroupLayout;
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
import javax.swing.text.DefaultCaret;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.http.message.BasicNameValuePair;
import org.apache.solr.common.SolrDocument;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JMapPane;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.junit.runner.manipulation.Filter;
import org.opengis.filter.IncludeFilter;
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
    static boolean debug = true;

    private static final long serialVersionUID = -2714378087612244399L;
    private static final String SOLR_URL = "http://localhost:8983/solr/Billualizer";


    final static File SHAPE_FILE = new File(new File(new File(System.getProperty("user.dir"), "assets"), "states_shp"), "states.shp");
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

    static JTextField searchBar;
    static ButtonGroup searchTypeRadioGroup;
    static JRadioButton documentSearch;
    static JRadioButton assemblySearch;
    static JRadioButton stateSearch;
    static JRadioButton nationalSearch;

    static Mappa map;
    static JMapPane mapPane;   

    static HashMap<String, ArrayList<String>> directoryToURL;
    static HashMap<String, ArrayList<String>> fileNameToURL;

    final static String newline = "\n";
    static String selectedAssemblyName;
    static String selectedHouseName;
    static String selectedDocumentName;

    static File masterDir;

    public BillExplorer() {
        super(new BorderLayout());

        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(3,20);
        log.setMargin(new Insets(2,2,2,2));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setMinimumSize(new Dimension(100, 20));
        logScrollPane.setPreferredSize(new Dimension(800, 30));
        DefaultCaret caret = (DefaultCaret) log.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

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
        directories.addTreeSelectionListener(this);
        directories.setVisibleRowCount(3);
        JScrollPane directoriesScrollPane = new JScrollPane(directories);
        directoriesScrollPane.setMinimumSize(new Dimension(100, 200));
        directoriesScrollPane.setPreferredSize(new Dimension(200, 500));
        directoriesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        directoriesScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        directoriesScrollPane.setViewportView(directories);

        //State list:
        stateList = new JList<String>(new String[1]);
        stateList.addListSelectionListener(this);
        stateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stateList.setLayoutOrientation(JList.VERTICAL_WRAP);
        stateList.setPreferredSize(new Dimension(300, 30));
        stateList.setMaximumSize(new Dimension(600, 30));
        stateList.setVisibleRowCount(1);
        JScrollPane stateListScrollPane = new JScrollPane(stateList);
        stateListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        stateListScrollPane.setPreferredSize(new Dimension(300, 30));
        stateListScrollPane.setMaximumSize(new Dimension(650, 30));

        //Set up tab pane:
        tabPane = new JTabbedPane();
        tabPane.setMinimumSize(new Dimension(200, 200));
        tabPane.setPreferredSize(new Dimension(400, 500));
        populateTabPane();

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));
        centerPanel.setMinimumSize(new Dimension(200, 200));
        centerPanel.setPreferredSize(new Dimension(500, 500));
        centerPanel.add(stateListScrollPane);
        centerPanel.add(tabPane);

        JSplitPane masterSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, directoriesScrollPane, centerPanel);
        masterSplitPane.setMinimumSize(new Dimension(500, 200));
        masterSplitPane.setPreferredSize(new Dimension(600, 600));
        masterSplitPane.setDividerLocation(200);

        JSplitPane metaSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, masterSplitPane, logScrollPane);
        metaSplitPane.setDividerLocation(570);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(metaSplitPane, BorderLayout.LINE_START);  
    }

    /**
     * Removes all tabs from tabPane and generates each of them again
     * programmatically.
     */
    private void populateTabPane() {
        //Create JSON module:
        jsonViewer = new JTextArea(20, 20);
        jsonViewer.setMinimumSize(new Dimension(200, 150));
        jsonViewer.setPreferredSize(new Dimension(250, 200));
        jsonViewer.setMargin(new Insets(5, 5, 5, 5));
        jsonViewer.setEditable(false);
        JScrollPane jsonViewerScrollPane = new JScrollPane(jsonViewer);
        //        jsonViewerScrollPane.setPreferredSize(new Dimension(200, 200));
        tabPane.addTab("JSON", jsonViewerScrollPane);

        //Create bill viewer module:
        billsViewer = new JEditorPane();
        billsViewer.setMinimumSize(new Dimension(200, 150));
        billsViewer.setPreferredSize(new Dimension(400, 450));
        billsViewer.setMargin(new Insets(5, 5, 5, 5));
        billsViewer.setEditable(false);
        JScrollPane billsViewerScrollPane = new JScrollPane(billsViewer);
        tabPane.addTab("Bills", billsViewerScrollPane);

        //Create text tab:
        JTextArea textViewer = new JTextArea(20, 20);
        textViewer.setPreferredSize(new Dimension(200, 150));
        textViewer.setMargin(new Insets(5, 5, 5, 5));
        textViewer.setEditable(false);
        JScrollPane textViewerScrollPane = new JScrollPane(textViewer);
        tabPane.addTab("Text", textViewerScrollPane);

        //Create search tab:
        JPanel searchPanel = new JPanel(new BorderLayout());

        //Create GroupLayout:

        indexButton = new JButton("Index State");
        indexButton.addActionListener(this);

        searchBar = new JTextField(8);

        searchButton = new JButton("Search");
        searchButton.addActionListener(this);

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

        JPanel searchModule = new JPanel();
        JLabel label = new JLabel("Search: ");
        GroupLayout groupLayout = new GroupLayout(searchModule);
        GroupLayout.SequentialGroup hGroup = groupLayout.createSequentialGroup().addComponent(indexButton)
                .addComponent(label).addComponent(searchBar).addComponent(searchButton)
                .addComponent(documentSearch).addComponent(assemblySearch).addComponent(stateSearch).addComponent(nationalSearch);
        groupLayout.setHorizontalGroup(hGroup);

        GroupLayout.ParallelGroup vGroup = groupLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(indexButton)
                .addComponent(label).addComponent(searchBar).addComponent(searchButton)
                .addComponent(documentSearch).addComponent(assemblySearch).addComponent(stateSearch).addComponent(nationalSearch);
        groupLayout.setVerticalGroup(vGroup);
        searchModule.setLayout(groupLayout);
        searchPanel.add(searchModule, BorderLayout.PAGE_START);

        //Map:
        map = new Mappa(SHAPE_FILE);
        mapPane = new JMapPane(map);
        searchPanel.add(mapPane, BorderLayout.CENTER);

        tabPane.addTab("Search", searchPanel);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //        //creating and showing this application's GUI.
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
            String searchText = searchBar.getText();
            SolrInteractor interactor = new SolrInteractor(SOLR_URL);

            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            if(! nationalSearch.isSelected()) {
                params.add(new BasicNameValuePair("state", stateList.getSelectedValue()));
                if(! stateSearch.isSelected()) {
                    params.add(new BasicNameValuePair("assembly", selectedAssemblyName));
                    if(! assemblySearch.isSelected()) {
                        params.add(new BasicNameValuePair("title", selectedDocumentName));
                    }
                }
            }

            try {
                List<SolrDocument> results = interactor.query(searchText, params);
                List<String> states = new ArrayList<String>();
                List<String> titles = new ArrayList<String>();
                for(SolrDocument doc : results) {
                    if(!states.contains(doc.getFieldValue("state")))
                        states.add((String) doc.getFieldValue("state"));
                    titles.add((String) doc.getFieldValue("title"));
                }
                System.out.println(titles);
                System.out.println(states);
                log.append("Found " + titles.size() + " match(es) in " + states.size() + " state(s)." + newline); 
                map.setColoredStates(states);
            } catch (/*SolrServerException | IO*/Exception e1) {
                if(debug) e1.printStackTrace();
            }
        }
        else if(e.getSource() == indexButton) {
            new Thread() {
                @Override
                public void run() {
                    SolrInteractor interactor = new SolrInteractor(SOLR_URL);
                    interactor.indexState(new File(masterDir, stateList.getSelectedValue()));
                }
            }.start();
        }
    }        

    public static HashMap<String, ArrayList<String>> loadPickleList(File pickle) {
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

    public static HashMap<String, String> loadPickleString(File pickle) {
        HashMap<String, String> pickleMap = new HashMap<String, String>();
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
            PyString countryIdList = (PyString) entry.getValue();
            pickleMap.put(appId, countryIdList.asString());
        }
        return pickleMap;
    }


    @Override
    public void valueChanged(TreeSelectionEvent arg0) {
        if(arg0.getSource() == directories) {
            updatePath(arg0);

            //Update tabs:
            if(((FileTreeNode) arg0.getPath().getLastPathComponent()).isLeaf()) {
                try {
                    log.append("Looking for: " + ((FileTreeNode) directories.getSelectionPath().getLastPathComponent()).getTitle() + newline);
                    ArrayList<String> versions = directoryToURL.get(buildFileFromTreePath(directories.getSelectionPath()));
                    billsViewer.setPage(versions.get(versions.size()-1));
                } catch (IOException e) {
                    log.append("Invalid URL." + newline);
                    if(debug) e.printStackTrace();
                } catch (NullPointerException e) {
                    log.append("NPE. Check connectivity." + newline);
                    if(debug) e.printStackTrace();
                }

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

    /**
     * Updates the 'currently selected' fields with a new TreeSelectionEvent
     * @param e the TreeSelectionEvent which triggered the update
     */
    public void updatePath(TreeSelectionEvent e) {
        TreePath path = e.getPath();
        if(path.getPathCount() == 4) {
            selectedDocumentName = path.getLastPathComponent().toString();
            path = path.getParentPath();
            if(debug) System.out.println("Selected Document Name: " + selectedDocumentName);
        }
        if(path.getPathCount() == 3) {
            selectedHouseName = path.getLastPathComponent().toString();
            path = path.getParentPath();
            if(debug) System.out.println("Selected House Name: " + selectedHouseName);
        }
        if(path.getPathCount() == 2) {
            selectedAssemblyName = path.getLastPathComponent().toString();
            if(debug) System.out.println("Selected Assembly Name: " + selectedAssemblyName);
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

    public String buildFileFromTreePath(TreePath path) {
        int counter = 1;
        StringBuilder builder = new StringBuilder("json/bills/");
        builder.append(stateList.getSelectedValue().toLowerCase());

        while(true) {
            try {
                builder.append("/" + ((FileTreeNode) path.getPathComponent(counter)).toString());
                counter++;
            } catch(IllegalArgumentException e) {
                break;
            }
        }

        return builder.toString();
    }

}