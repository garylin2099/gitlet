package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/** Commit class.
 * @author Yizhang Lin
 * */
public class Commit implements Serializable {
    /** initialize Commit.
     * @param d commit date
     * @param l commit log*/
    public Commit(Date d, String l) {
        date = d;
        log = l;
        files = new HashMap<String, String>();
    }
    /** get date.
     * @return date the commit is made*/
    public Date getDate() {
        return date;
    }
    /** get log.
     * @return commit log as a string*/
    public String getLog() {
        return log;
    }
    /** get parent commit.
     * @return a string representing the parent commit */
    public String getParentCommit() {
        return parentCommit;
    }
    /** get all tracked files.
     * @return tracked files */
    public HashMap<String, String> getFiles() {
        return files;
    }
    /** set parent commit of this.
     * @param p the string for parent commit*/
    public void setParentCommit(String p) {
        parentCommit = p;
    }
    /** commit date. */
    private Date date;
    /** commit log. */
    private String log;
    /** tracked files, key is file name, value is file hash name. */
    private HashMap<String, String> files;
    /** parent commit. */
    private String parentCommit;
}
