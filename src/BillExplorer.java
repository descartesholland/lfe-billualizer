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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.python.core.PyDictionary;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.modules.cPickle;

/**
 * @author android
 *
 */
public class BillExplorer extends JPanel implements ActionListener, ListSelectionListener {
    boolean debug = false;
    
    private static final long serialVersionUID = -2714378087612244399L;
    JButton openButton;
    JTextArea log;
    JFileChooser fc;
    JList<String> stateList;
    JList<String> directories;
    JEditorPane billsViewer;

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
        directories = new JList<String>(billNames);
        directories.setSelectedIndex(0);
        directories.addListSelectionListener(this);
        JScrollPane directoriesScrollPane = new JScrollPane(directories);

        
        //Main pane tabs:
        JTabbedPane tabPane = new JTabbedPane();

        JTextArea jsonViewer = new JTextArea(100, 200);
        jsonViewer.setMargin(new Insets(5, 5, 5, 5));
        jsonViewer.setEditable(false);
        JScrollPane jsonViewerScrollPane = new JScrollPane(jsonViewer);
        tabPane.addTab("JSON", jsonViewerScrollPane);

        //Create bill viewer module:
        billsViewer = new JEditorPane();
//        try {
//            billsViewer.setPage(fileNameToURL.get(directories.getSelectedValue()).get(0));
//        } catch (IOException e) {
//            log.append("Invalid URL." + newline);
//            if(debug) e.printStackTrace();
//        }
        JScrollPane billsViewerScrollPane = new JScrollPane(billsViewer);
//        JSplitPane billsViewerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, directoriesScrollPane, billsViewerScrollPane);
        
        tabPane.addTab("Bills", billsViewerScrollPane);
        
        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, directoriesScrollPane, tabPane);
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
        frame.pack();
        frame.setMinimumSize(new Dimension(500, 600));
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //Handle open button action.
        if(e.getSource() == openButton) {
            int returnVal = fc.showOpenDialog(BillExplorer.this);

            if(returnVal == JFileChooser.APPROVE_OPTION) {
                masterDir = fc.getSelectedFile();
                //This is where a real application would open the file.
                log.append("Opening: " + masterDir.getName() + "." + newline);

                DefaultListModel<String> model = new DefaultListModel<String>();
                for(String fileName : masterDir.list())
                    model.addElement(fileName);
                stateList.setModel(model);
                
                ListSelectionModel listSel = new DefaultListSelectionModel();
                listSel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                stateList.setSelectionModel(listSel);
                stateList.setSelectedIndex(0);
            } 
            else
                log.append("Open command cancelled by user." + newline);
            log.setCaretPosition(log.getDocument().getLength());
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
        //        System.out.println(idToCountries.toString());
        return pickleMap;
    }

    @Override
    public void valueChanged(ListSelectionEvent arg0) {
        if(arg0.getSource() == directories) {
            try {
                billsViewer.setPage(fileNameToURL.get(directories.getSelectedValue()).get(0));
            } catch (IOException e) {
                log.append("Invalid URL." + newline);
                if(debug) e.printStackTrace();
            }
        }
    }
}
