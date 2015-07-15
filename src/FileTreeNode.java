import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 
 */

/**
 * @author android
 *
 */
public class FileTreeNode implements Comparable<FileTreeNode> {
    private ArrayList<FileTreeNode> children;
    private File file;
    private String title;

    public FileTreeNode(File file, String name) {
        this.file = file;
        this.title = name;

        children = new ArrayList<FileTreeNode>();
        loadChildren();
    }

    private void loadChildren() {
        if(file.listFiles() != null) {
            for(File f : file.listFiles())
                children.add(new FileTreeNode(f, f.getName()));
            Collections.sort(children);
        }
    }

    public boolean isLeaf() {
        if(file.isFile()) return true;
        if(file.isDirectory() && file.listFiles() != null && file.listFiles().length > 0)
            return false;
        return true;
    }

    public FileTreeNode getParent() {
        return new FileTreeNode(this.file.getParentFile(), this.file.getParentFile().getName());
    }

    public FileTreeNode getChildAt(int index) {
        return children.get(index);
    }

    public String getTitle() {
        return this.title;
    }

    public File getFile() {
        return this.file;
    }
    
    public int getChildCount() {
        return this.children.size();
    }

    @Override
    public int compareTo(FileTreeNode another) {
        return this.title.compareTo(another.getTitle());
    }

    @Override
    public String toString() {
        return this.title;
    }
}
