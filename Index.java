package gitlet;

import java.io.Serializable;
import java.util.HashMap;

/** Index class representing the staged area.
 * @author Yizhang Lin
 * */
public class Index implements Serializable {
    /** initialize Index. */
    public Index() {
        allAddedFiles = new HashMap<String, String>();
    }

    /** get staged files.
     * @return all staged files in a hash map
     * */
    public HashMap<String, String> getAllAddedFiles() {
        return allAddedFiles;
    }

    /** all staged files in fileName - hashFileName pair.*/
    private HashMap<String, String> allAddedFiles;
}
