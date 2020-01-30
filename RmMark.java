package gitlet;

import java.io.Serializable;
import java.util.ArrayList;

/** Class recording files marked removal.
 * @author Yizhang Lin */
public class RmMark implements Serializable {
    /** initialize RmMark. */
    public RmMark() {
        filesToRm = new ArrayList<String>();
    }

    /** marked a file as removed.
     * @param file file to mark as removed*/
    public void addMark(String file) {
        filesToRm.add(file);
    }
    /** get names of all removed files.
     * @return all file names of removed files*/
    public ArrayList<String> getFilesToRm() {
        return filesToRm;
    }
    /** file names of all file removed. */
    private ArrayList<String> filesToRm;
}
