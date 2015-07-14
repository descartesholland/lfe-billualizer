import java.util.ArrayList;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * 
 */

/**
 * @author android
 *
 */
public class MyTreeModel implements TreeModel {
    private ArrayList<TreeModelListener> treeModelListeners = new ArrayList<TreeModelListener>();
    private FileTreeNode root;
    
    public MyTreeModel(FileTreeNode root) {
        this.root = root;
    }
    
    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
     */
    @Override
    public void addTreeModelListener(TreeModelListener arg0) {
        treeModelListeners.add(arg0);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
     */
    @Override
    public Object getChild(Object arg0, int arg1) {
        return ((FileTreeNode) arg0).getChildAt(arg1);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
     */
    @Override
    public int getChildCount(Object arg0) {
        return ((FileTreeNode) arg0).getChildCount();
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
     */
    @Override
    public int getIndexOfChild(Object arg0, Object arg1) {
        
        return 0;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#getRoot()
     */
    @Override
    public Object getRoot() {
        return this.root;
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
     */
    @Override
    public boolean isLeaf(Object arg0) {
        return ((FileTreeNode) arg0).isLeaf();
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
     */
    @Override
    public void removeTreeModelListener(TreeModelListener arg0) {
        treeModelListeners.remove(arg0);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
     */
    @Override
    public void valueForPathChanged(TreePath arg0, Object arg1) {
        // TODO Auto-generated method stub

    }

}
