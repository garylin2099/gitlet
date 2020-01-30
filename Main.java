package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Yizhang Lin
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");

    /** Main metadata folder. */
    static final File GIT_FOLDER = Utils.join(CWD, ".gitlet");
    /** filese folder. */
    static final File FILES = Utils.join(GIT_FOLDER, "files");
    /** commit folder. */
    static final File COMMIT = Utils.join(GIT_FOLDER, "commit");
    /** branch head folder. */
    static final File BRANCH_HEAD = Utils.join(GIT_FOLDER, "branchHeads");
    /** current branch head points to. */
    static final File CUR_BRANCH = Utils.join(GIT_FOLDER, "HEAD");
    /** staged area status. */
    static final File INDEX = Utils.join(GIT_FOLDER, "index");
    /** all logs. */
    static final File ALL_LOG = Utils.join(GIT_FOLDER, "allLogs");
    /** remove marks. */
    static final File RM_MARK = Utils.join(GIT_FOLDER, "rmMark");
    /** init commit year. */
    static final int START_YEAR = 1970;


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args.length == 0) {
            Utils.message("Please enter a command.");
            System.exit(0);
        }
        switch (args[0]) {
        case "init":
            init();
            break;
        case "add":
            add(args);
            break;
        case "commit":
            commit(args);
            break;
        case "branch":
            branch(args);
            break;
        case "checkout":
            checkout(args);
            break;
        case "log":
            log();
            break;
        case "global-log":
            globalLog();
            break;
        case "rm":
            rm(args);
            break;
        case "find":
            find(args);
            break;
        case "status":
            status();
            break;
        case "rm-branch":
            rmBranch(args);
            break;
        case "reset":
            reset(args);
            break;
        case "merge":
            merge(args);
            break;
        default:
            break;

        }

    }

    /** initialize gitlet. */
    private static void init() {
        if (GIT_FOLDER.exists()) {
            Utils.message("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }
        GIT_FOLDER.mkdir();
        COMMIT.mkdir();
        FILES.mkdir();
        BRANCH_HEAD.mkdir();
        Date commitDt = new GregorianCalendar(START_YEAR, 0, 1).getTime();

        Commit c = new Commit(commitDt, "initial commit");

        String cHashName = Utils.sha1(Utils.serialize(c));
        Utils.writeObject(Utils.join(COMMIT, cHashName), c);
        Utils.writeContents(Utils.join(BRANCH_HEAD, "master"), cHashName);
        Utils.writeContents(CUR_BRANCH, "master");
        Utils.writeObject(INDEX, new Index());
        String initLog = "===\ncommit " + cHashName + "\nDate: "
                + String.format(
                        "%1$ta %1$tb %1$te %1$tH:%1$tM:%1$tS %1$tY %1$tz",
                commitDt)
                + "\ninitial commit";
        Utils.writeContents(ALL_LOG, initLog);
        Utils.writeObject(RM_MARK, new RmMark());
    }

    /** add helper function.
     * @param args files to add
     * */
    private static void add(String... args) {
        File fileToAdd = Utils.join(CWD, args[1]);
        if (!fileToAdd.exists()) {
            Utils.message("File does not exist.");
            System.exit(0);
        }
        Commit head = getHead();
        Index currIndex = Utils.readObject(INDEX, Index.class);
        String fHashName = getHashName(fileToAdd);
        if (head.getFiles().containsValue(fHashName)) {
            if (!currIndex.getAllAddedFiles().isEmpty()
                    && currIndex.getAllAddedFiles().containsKey(args[1])) {
                currIndex.getAllAddedFiles().remove(fileToAdd);
            }
            System.exit(0);
        }

        RmMark rmMark = Utils.readObject(RM_MARK, RmMark.class);
        if (rmMark.getFilesToRm().contains(args[1])) {
            rmMark.getFilesToRm().remove(args[1]);
            Utils.writeObject(RM_MARK, rmMark);
            return;
        }

        Utils.writeContents(Utils.join(FILES, fHashName),
                Utils.readContents(fileToAdd));
        currIndex.getAllAddedFiles().put(args[1], fHashName);
        Utils.writeObject(INDEX, currIndex);
    }

    /** commit helper function.
     * @param args files to commit
     * */
    private static void commit(String... args) {
        Index stageFiles = Utils.readObject(INDEX, Index.class);
        RmMark rmMark = Utils.readObject(RM_MARK, RmMark.class);
        if (args.length == 1 || args[1].equals("")) {
            Utils.message("Please enter a commit message.");
            System.exit(0);
        }
        if (stageFiles.getAllAddedFiles().isEmpty()
                && rmMark.getFilesToRm().isEmpty()) {
            Utils.message("No changes added to the commit.");
            System.exit(0);
        }
        Date commitDt = new Date();
        Commit newCommit = new Commit(commitDt, args[1]);
        newCommit.setParentCommit(getHeadHashName());

        Commit parentCommit = getHead();
        HashMap<String, String> parentFiles = parentCommit.getFiles();

        ArrayList<String> filesToRm = rmMark.getFilesToRm();
        if (!filesToRm.isEmpty()) {
            for (String fileName : filesToRm) {
                parentFiles.remove(fileName);
            }
        }
        newCommit.getFiles().putAll(parentFiles);
        newCommit.getFiles().putAll(stageFiles.getAllAddedFiles());
        String newCommitHash = getObjHashName(newCommit);
        Utils.writeObject(Utils.join(COMMIT, newCommitHash), newCommit);

        Utils.writeContents(getHeadFile(), newCommitHash);

        stageFiles.getAllAddedFiles().clear();
        Utils.writeObject(INDEX, stageFiles);

        filesToRm.clear();
        Utils.writeObject(RM_MARK, rmMark);

        String commitLog = "===\ncommit " + newCommitHash + "\nDate: "
                + String.format(
                        "%1$ta %1$tb %1$te %1$tH:%1$tM:%1$tS %1$tY %1$tz",
                commitDt)
                + "\n" + args[1];
        Utils.writeContents(ALL_LOG,
                commitLog + "\n\n" + Utils.readContentsAsString(ALL_LOG));

    }

    /** branch.
     * @param args branch to checkout
     * */
    private static void branch(String... args) {
        List<String> branchNm = Utils.plainFilenamesIn(BRANCH_HEAD);
        if (branchNm.contains(args[1])) {
            Utils.message("A branch with that name already exists.");
            System.exit(0);
        }
        File newBranchHead = Utils.join(BRANCH_HEAD, args[1]);
        Utils.writeContents(newBranchHead, getHeadHashName());
    }

    /** checkout.
     * @param args commit id, files, or branch to check out
     * */
    private static void checkout(String... args) {
        Commit currCommit = getHead();
        HashMap<String, String> currComFiles = currCommit.getFiles();
        switch (args.length) {
        case 3:
            if (!currComFiles.containsKey(args[2])) {
                Utils.message("File does not exist in that commit.");
                System.exit(0);
            }
            File ckFileHashName = Utils.join(FILES, currComFiles.get(args[2]));
            Utils.writeContents(Utils.join(CWD, args[2]),
                    Utils.readContents(ckFileHashName));

            break;
        case 4:
            if (!args[2].equals("--")) {
                Utils.message("Incorrect operands.");
                System.exit(0);
            }
            checkComIdExist(args[1]);
            Commit comThisID = Utils.readObject(
                    Utils.join(COMMIT, args[1]), Commit.class);
            HashMap<String, String> filesThisID = comThisID.getFiles();
            if (!filesThisID.containsKey(args[3])) {
                Utils.message("File does not exist in that commit.");
                System.exit(0);
            }
            File ckFileHashName2 = Utils.join(FILES, filesThisID.get(args[3]));
            Utils.writeContents(Utils.join(CWD, args[3]),
                    Utils.readContents(ckFileHashName2));
            break;
        case 2:
            List<String> allBranchNames = Utils.plainFilenamesIn(BRANCH_HEAD);
            if (!allBranchNames.contains(args[1])) {
                Utils.message("No such branch exists.");
                System.exit(0);
            }
            if (Utils.readContentsAsString(CUR_BRANCH).equals(args[1])) {
                Utils.message("No need to checkout the current branch.");
                System.exit(0);
            }
            checkoutBranch(currComFiles,
                    Utils.readContentsAsString(
                            Utils.join(BRANCH_HEAD, args[1])));
            Utils.writeContents(CUR_BRANCH, args[1]);
            break;
        default:
            break;
        }
    }

    /** log helper. */
    private static void log() {
        Commit head = getHead();
        String headID = getHeadHashName();
        System.out.println(printLog(head, headID) + "\n");
        while (head.getParentCommit() != null) {
            String parentID = head.getParentCommit();
            head = Utils.readObject(Utils.join(COMMIT, parentID), Commit.class);
            System.out.println(printLog(head, parentID) + "\n");
        }
    }

    /** globalLog helper. */
    private static void globalLog() {
        System.out.println(Utils.readContentsAsString(ALL_LOG) + "\n");
    }

    /** remove helper.
     * @param args files to remove
     * */
    private static void rm(String... args) {
        String fileToRm = args[1];
        Index currIndex = getIndex();
        HashMap<String, String> stagedFiles = currIndex.getAllAddedFiles();
        Commit head = getHead();
        HashMap<String, String> headFiles = head.getFiles();
        if (!stagedFiles.containsKey(fileToRm)
                && !headFiles.containsKey(fileToRm)) {
            Utils.message("No reason to remove the file.");
            System.exit(0);
        }
        if (stagedFiles.containsKey(fileToRm)) {
            stagedFiles.remove(fileToRm);
            Utils.writeObject(INDEX, currIndex);
        }
        if (headFiles.containsKey(fileToRm)) {
            RmMark rmMark = Utils.readObject(RM_MARK, RmMark.class);
            rmMark.addMark(fileToRm);
            Utils.writeObject(RM_MARK, rmMark);
            if (Utils.join(CWD, fileToRm).exists()) {
                Utils.restrictedDelete(Utils.join(CWD, fileToRm));
            }
        }
    }

    /** find.
     * @param args commit message to find
     * */
    private static void find(String... args) {
        String allCommit = Utils.readContentsAsString(ALL_LOG);
        String[] commitArr = allCommit.split("===");
        boolean found = false;
        for (int i = 0; i < commitArr.length; i++) {
            if (commitArr[i].indexOf("\n" + args[1] + "\n\n") >= 0) {
                if (found) {
                    System.out.println();
                }
                found = true;
                String[] commitElement = commitArr[i].split("[ \\n]");
                System.out.print(commitElement[2]);
            }
        }
        if (!found) {
            Utils.message("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** status helper. */
    private static void status() {
        String output = "";
        String curBranch = getCurrBranch();
        TreeSet<String> branches = new TreeSet<String>();
        TreeSet<String> staged = new TreeSet<String>();
        TreeSet<String> removed = new TreeSet<String>();
        TreeSet<String> modified = new TreeSet<String>();
        TreeSet<String> untracked = new TreeSet<String>();
        branches.addAll(Utils.plainFilenamesIn(BRANCH_HEAD));
        output += "=== Branches ===\n";
        Iterator<String> branchNm = branches.iterator();
        while (branchNm.hasNext()) {
            String b = branchNm.next();
            if (b.equals(curBranch)) {
                output += "*" + b + "\n";
            } else {
                output += b + "\n";
            }
        }
        output += "\n=== Staged Files ===\n";
        HashMap<String, String> addedFiles = getIndex().getAllAddedFiles();
        staged.addAll(addedFiles.keySet());
        Iterator<String> stagedNm = staged.iterator();
        while (stagedNm.hasNext()) {
            output += stagedNm.next() + "\n";
        }
        output += "\n=== Removed Files ===\n";
        RmMark rmMark = Utils.readObject(RM_MARK, RmMark.class);
        removed.addAll(rmMark.getFilesToRm());
        Iterator<String> rmNm = removed.iterator();
        while (rmNm.hasNext()) {
            output += rmNm.next() + "\n";
        }

        List<String> allWorkFiles = Utils.plainFilenamesIn(CWD);
        HashMap<String, String> headFiles = getHead().getFiles();
        output += "\n=== Modifications Not Staged For Commit ===\n";

        output += "\n=== Untracked Files ===\n";
        for (String f : allWorkFiles) {
            if (!headFiles.containsKey(f) && !addedFiles.containsKey(f)) {
                untracked.add(f);
            } else if (headFiles.containsKey(f) && !addedFiles.containsKey(f)
                    && rmMark.getFilesToRm().contains(f)) {
                untracked.add(f);
            }
        }
        Iterator<String> untrk = untracked.iterator();
        while (untrk.hasNext()) {
            output += untrk.next() + "\n";
        }

        System.out.print(output);

    }

    /** remove branch helper.
     * @param args branch to remove
     * */
    private static void rmBranch(String... args) {
        List<String> allBranchNames = Utils.plainFilenamesIn(BRANCH_HEAD);
        if (!allBranchNames.contains(args[1])) {
            Utils.message("A branch with that name does not exist.");
            System.exit(0);
        }
        if (Utils.readContentsAsString(CUR_BRANCH).equals(args[1])) {
            Utils.message("Cannot remove the current branch.");
            System.exit(0);
        }
        File branchToDel = Utils.join(BRANCH_HEAD, args[1]);
        branchToDel.delete();
    }

    /** reset helper.
     * @param args commit id to reset to
     * */
    private static void reset(String... args) {
        checkComIdExist(args[1]);
        Commit currCommit = getHead();
        HashMap<String, String> currComFiles = currCommit.getFiles();
        checkoutBranch(currComFiles, args[1]);
    }

    /** merge helper.
     * @param args branch to merge
     * */
    private static void merge(String... args) {
        Index currIndex = getIndex();
        RmMark rmMark = Utils.readObject(RM_MARK, RmMark.class);
        if (!currIndex.getAllAddedFiles().isEmpty()
                || !rmMark.getFilesToRm().isEmpty()) {
            Utils.message("You have uncommitted changes.");
            System.exit(0);
        }

        List<String> allBranchNames = Utils.plainFilenamesIn(BRANCH_HEAD);
        if (!allBranchNames.contains(args[1])) {
            Utils.message("A branch with that name does not exist.");
            System.exit(0);
        }

        if (getCurrBranch().equals(args[1])) {
            Utils.message("Cannot merge a branch with itself.");
            System.exit(0);
        }

        String switchToCmtStr = Utils.readContentsAsString(
                Utils.join(BRANCH_HEAD, args[1]));
        Commit switchTo = Utils.readObject(Utils.join(COMMIT, switchToCmtStr),
                Commit.class);
        HashMap<String, String> switchToFiles = switchTo.getFiles();
        HashMap<String, String> stagedFiles = currIndex.getAllAddedFiles();
        checkUntrackedExist(getHead().getFiles(), stagedFiles, switchToFiles);


    }

    /** get the location of the head commit.
     * @return a file indicating the location
     * */
    private static File getHeadFile() {
        String currBranch = Utils.readContentsAsString(CUR_BRANCH);
        return Utils.join(BRANCH_HEAD, currBranch);
    }

    /** get the hash string of head commit.
     * @return hash stirng of the head commit
     * */
    private static String getHeadHashName() {
        return Utils.readContentsAsString(getHeadFile());
    }

    /** get head.
     * @return the head commit object
     * */
    private static Commit getHead() {
        String head = getHeadHashName();
        return Utils.readObject(Utils.join(COMMIT, head), Commit.class);
    }

    /** get hash name of a file.
     * @param f files to hash
     * @return hash string of the file
     * */
    private static String getHashName(File f) {
        return Utils.sha1(Utils.readContents(f));
    }

    /** get hash name of an object.
     * @param obj object to hash
     * @return hash string of that object
     * */
    private static String getObjHashName(Serializable obj) {
        return Utils.sha1(Utils.serialize(obj));
    }

    /** get the staged area.
     * @return Index object representing the staged area
     * */
    private static Index getIndex() {
        return Utils.readObject(INDEX, Index.class);
    }

    /** get current branch.
     * @return branch name*/
    private static String getCurrBranch() {
        return Utils.readContentsAsString(CUR_BRANCH);
    }

    /** print log helper.
     * @param c Commit to get info to print out
     * @param id commit id
     * @return log information
     * */
    private static String printLog(Commit c, String id) {
        return "===\ncommit " + id + "\nDate: "
                + String.format(
                        "%1$ta %1$tb %1$te %1$tH:%1$tM:%1$tS %1$tY %1$tz",
                c.getDate())
                + "\n" + c.getLog();
    }

    /** check if commit id exists.
     * @param id commit id
     * */
    private static void checkComIdExist(String id) {
        List<String> commitID = Utils.plainFilenamesIn(COMMIT);
        if (!commitID.contains(id)) {
            Utils.message("No commit with that id exists.");
            System.exit(0);
        }
    }

    /** checkout helper.
     * @param currCommitFiles files tracked by current commit
     * @param switchToCmtStr commit to switch to
     * */
    private static void checkoutBranch(HashMap<String, String> currCommitFiles,
                                       String switchToCmtStr) {
        Commit switchTo = Utils.readObject(Utils.join(COMMIT, switchToCmtStr),
                Commit.class);
        HashMap<String, String> switchToFiles = switchTo.getFiles();
        Index currIndex = getIndex();
        HashMap<String, String> stagedFiles = currIndex.getAllAddedFiles();

        checkUntrackedExist(currCommitFiles, stagedFiles, switchToFiles);

        for (String f : currIndex.getAllAddedFiles().keySet()) {
            Utils.restrictedDelete(Utils.join(CWD, f));
        }
        currIndex.getAllAddedFiles().clear();
        for (String f : currCommitFiles.keySet()) {
            Utils.restrictedDelete(Utils.join(CWD, f));
        }
        for (String f : switchToFiles.keySet()) {
            File toFileLocation = Utils.join(FILES, switchToFiles.get(f));
            Utils.writeContents(Utils.join(CWD, f),
                    Utils.readContents(toFileLocation));
        }
        Utils.writeObject(INDEX, currIndex);
    }

    /** check untracked files helper.
     * @param currentComFiles files tracked by current commit
     * @param addedFiles files in staged area
     * @param toFiles operation destination filse
     * */
    private static void checkUntrackedExist(
            HashMap<String, String> currentComFiles,
            HashMap<String, String> addedFiles,
            HashMap<String, String> toFiles) {

        List<String> allWorkFiles = Utils.plainFilenamesIn(CWD);
        for (String f : allWorkFiles) {
            if (f.equals(".DS_Store")) {
                continue;
            }
            if (toFiles.containsKey(f)
                    && !currentComFiles.containsKey(f)
                    && !addedFiles.containsKey(f)) {
                Utils.message("There is an untracked file in the way;"
                        + " delete it or add it first.");
                System.exit(0);
            }
        }
    }
}
