package bugtracker;

import model.Release;
import model.Ticket;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.MethodMetrics;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.*;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

public class JiraBugFetcher {


//    public static Map<String, Set<String>> getModifiedMethodsFromBugCommits(Ticket ticket, File repoDir) {
//        Map<String, Set<String>> result = new HashMap<>();
//
//        try (Git git = Git.open(repoDir)) {
//            Repository repo = git.getRepository();
//
//            Iterable<RevCommit> logs = git.log().call();
//            for (RevCommit commit : logs) {
//                if (!commit.getFullMessage().contains(ticket.getTicketKey())) continue;
//
//                ticket.addCommit(commit);
//
//                RevCommit parent = commit.getParentCount() > 0 ? commit.getParent(0) : null;
//                if (parent == null) continue;
//
//                try (ObjectReader reader = repo.newObjectReader()) {
//                    CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
//                    oldTreeIter.reset(reader, parent.getTree());
//
//                    CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
//                    newTreeIter.reset(reader, commit.getTree());
//
//                    List<DiffEntry> diffs = git.diff()
//                            .setOldTree(oldTreeIter)
//                            .setNewTree(newTreeIter)
//                            .setShowNameAndStatusOnly(false)
//                            .call();
//
//                    DiffFormatter formatter = new DiffFormatter(new ByteArrayOutputStream());
//                    formatter.setRepository(repo);
//                    formatter.setDiffComparator(RawTextComparator.DEFAULT);
//                    formatter.setDetectRenames(true);
//
//                    for (DiffEntry entry : diffs) {
//                        if (!entry.getNewPath().endsWith(".java")) continue;
//
//                        String filePath = entry.getNewPath();
//                        result.putIfAbsent(filePath, new HashSet<>());
//
//                        EditList edits = formatter.toFileHeader(entry).toEditList();
//                        for (Edit edit : edits) {
//                            String diffText = Arrays.toString(formatter.toFileHeader(entry).getBuffer());
//                            extractModifiedMethodNames(diffText, result.get(filePath));
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("❌ Errore durante l'analisi con JGit: " + e.getMessage());
//        }
//        //System.out.println("CIAOOOOOOOOO"+result.size());
//        return result;
//    }
//
//    private static void extractModifiedMethodNames(String diffText, Set<String> methods) {
//        // Cerca linee aggiunte che assomigliano alla dichiarazione di un metodo
//        Pattern methodPattern = Pattern.compile("^\\+.*\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\([^)]*\\)\\s*\\{?");
//        Scanner scanner = new Scanner(diffText);
//
//        while (scanner.hasNextLine()) {
//            String line = scanner.nextLine();
//            if (!line.startsWith("+") || line.startsWith("++")) continue;
//
//            Matcher matcher = methodPattern.matcher(line);
//            if (matcher.find()) {
//                String methodName = matcher.group(1);
//                methods.add(methodName);
//            }
//        }
//    }
    private static Map<String, Set<String>> getModifiedMethodsFromBugCommits(Ticket ticket, File repoDir) {
        Map<String, Set<String>> result = new HashMap<>();

        try {
            Process logProc = new ProcessBuilder("git", "log", "--pretty=format:%H", "--grep=" + ticket.getTicketKey())
                    .directory(repoDir)
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(logProc.getInputStream()));
            String commitHash;
            while ((commitHash = reader.readLine()) != null) {

                //ticket.addCommit(commitHash);

                Process showProc = new ProcessBuilder("git", "show", "--unified=0", commitHash)
                        .directory(repoDir)
                        .start();

                BufferedReader diffReader = new BufferedReader(new InputStreamReader(showProc.getInputStream()));

                String currentFile = null;
                String line;
                Pattern filePattern = Pattern.compile("^\\+\\+\\+ b/(.*\\.java)$");
                Pattern methodPattern = Pattern.compile("(?<=\\s)([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");

                while ((line = diffReader.readLine()) != null) {
                    Matcher fileMatcher = filePattern.matcher(line);
                    if (fileMatcher.find()) {
                        currentFile = fileMatcher.group(1).replace("\\", "/");
                        result.putIfAbsent(currentFile, new HashSet<>());
                    }

                    if (currentFile != null && line.startsWith("+")) {
                        Matcher m = methodPattern.matcher(line);
                        while (m.find()) {
                            String methodName = m.group(1);
                            result.get(currentFile).add(methodName);
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Errore nei comandi git: " + e.getMessage());
        }
        //System.out.println("reesult "+ result);
        return result;
    }

    public static Map<String, Set<String>> buildBuggyMethodMap1(List<Ticket> ticketList, String repoClonePath) {

        Map<String, Set<String>> releaseToBuggyMethods = new HashMap<>();

        for(Ticket ticket : ticketList) {

            Map<String, Set<String>> modified = getModifiedMethodsFromBugCommits(ticket, new File(repoClonePath));

            for (String file : modified.keySet()) {

                for (String method : modified.get(file)) {
                    String methodKey = file + "/" + method;

                    for (Release affected : ticket.getAv()) {
                        releaseToBuggyMethods.putIfAbsent(affected.getReleaseName(), new HashSet<>());
                        releaseToBuggyMethods.get(affected.getReleaseName()).add(methodKey);
                    }
                }
            }


        }

        return releaseToBuggyMethods;
    }


//    public static Map<String, Set<String>> buildBuggyMethodMap1(List<Ticket> ticketList, String repoClonePath) {
//        Map<String, Set<String>> releaseToBuggyMethods = new HashMap<>();
//
//        for (Ticket ticket : ticketList) {
//            // Ottieni i metodi modificati nei commit associati al ticket
//            Map<String, Set<String>> modified = getModifiedMethodsFromBugCommits(ticket, new File(repoClonePath));
//
//            for (Map.Entry<String, Set<String>> entry : modified.entrySet()) {
//                String file = entry.getKey();
//                Set<String> methods = entry.getValue();
//
//                for (String method : methods) {
//                    String methodKey = file + "/" + method;
//
//                    // Per ogni versione affetta (AV), registra il metodo come buggy
//                    for (Release affected : ticket.getAv()) {
//                        String releaseName = affected.getReleaseName();
//                        releaseToBuggyMethods
//                                .computeIfAbsent(releaseName, k -> new HashSet<>())
//                                .add(methodKey);
//                    }
//                }
//            }
//        }
//
//        return releaseToBuggyMethods;
//    }


    public static void computeChangeMetrics(MethodMetrics method, String methodName, String project, List<RevCommit> commits) throws IOException, InterruptedException {
        String filePath = method.getMethodPath();
        //String methodName = methodName; // da implementare se hai nomi interni

        List<RevCommit> commitList = filterCommitsModifyingMethod(commits, filePath, methodName, project);
        method.setMethodHistories(commits.size());

        Set<String> authors = new HashSet<>();
        int stmtAdded = 0;
        int stmtDeleted = 0;
        int condChanges = 0;
        int elseAdd = 0;
        int elseDel = 0;

        for (RevCommit commit : commitList) {
            authors.add(commit.getAuthorIdent().getName());

            String diff = getGitDiff(commit, filePath, project);
            stmtAdded += countLinesStartingWith(diff, "+");
            stmtDeleted += countLinesStartingWith(diff, "-");
            condChanges += countConditionChanges(diff);
            elseAdd += countPattern(diff, "\\+.*else");
            elseDel += countPattern(diff, "-.*else");

            System.out.println("numero autori"+authors.size());

           // method.setMethodHistories(methodHistories);
            method.setAuthors(authors.size());
            method.setStmtAdded(stmtAdded);
            method.setStmtDeleted(stmtDeleted);
            method.setChurn(stmtAdded + stmtDeleted);
            method.setElseAdded(elseAdd);
            method.setElseDeleted(elseDel);
            method.setCond(condChanges);
        }
    }

    public static List<RevCommit> findCommitsModifyingMethod(Repository repository,
                                                             List<RevCommit> commits,
                                                             String methodName) throws Exception {
        List<RevCommit> matchingCommits = new ArrayList<>();

        try (DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream())) {
            diffFormatter.setRepository(repository);

            for (RevCommit commit : commits) {
                if (commit.getParentCount() == 0) continue;

                RevCommit parent = commit.getParent(0);
                RevTree parentTree = parent.getTree();
                RevTree commitTree = commit.getTree();

                try (ObjectReader reader = repository.newObjectReader()) {
                    CanonicalTreeParser parentParser = new CanonicalTreeParser();
                    parentParser.reset(reader, parentTree);

                    CanonicalTreeParser commitParser = new CanonicalTreeParser();
                    commitParser.reset(reader, commitTree);

                    List<DiffEntry> diffs = diffFormatter.scan(parentParser, commitParser);

                    for (DiffEntry diff : diffs) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        try (DiffFormatter formatter = new DiffFormatter(out)) {
                            formatter.setRepository(repository);
                            formatter.format(diff);

                            String diffText = out.toString();

                            if (diffText.contains(methodName + "(")) { // semplice controllo sintattico
                                matchingCommits.add(commit);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return matchingCommits;
    }

    public static int countCommitsModifyingMethod(Repository repository, Git git, List<RevCommit> commits, String methodName) throws Exception {
        int count = 0;

        try (DiffFormatter diffFormatter = new DiffFormatter(new ByteArrayOutputStream())) {
            diffFormatter.setRepository(repository);

            for (RevCommit commit : commits) {
                if (commit.getParentCount() == 0) continue;

                RevCommit parent = commit.getParent(0);
                RevTree parentTree = parent.getTree();
                RevTree commitTree = commit.getTree();

                try (ObjectReader reader = repository.newObjectReader()) {
                    CanonicalTreeParser parentParser = new CanonicalTreeParser();
                    parentParser.reset(reader, parentTree);

                    CanonicalTreeParser commitParser = new CanonicalTreeParser();
                    commitParser.reset(reader, commitTree);

                    List<DiffEntry> diffs = diffFormatter.scan(parentParser, commitParser);

                    for (DiffEntry diff : diffs) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        try (DiffFormatter formatter = new DiffFormatter(out)) {
                            formatter.setRepository(repository);
                            formatter.format(diff);

                            String diffText = out.toString();

                            if (diffText.contains(methodName + "(")) {
                                count++;
                                break; // conta una sola volta per commit
                            }
                        }
                    }
                }
            }
        }

        return count;
    }

//    public void computeChangeMetrics(MethodMetrics method) throws IOException, InterruptedException {
//        String filePath = method.getMethodPath();
//        String methodName = extractMethodName(filePath); // da implementare se hai nomi interni
//
//        List<String> commits = getCommitsForMethod(filePath, methodName);
//        method.setMethodHistories(commits.size());
//
//        Set<String> authors = new HashSet<>();
//        int stmtAdded = 0;
//        int stmtDeleted = 0;
//        int condChanges = 0;
//        int elseAdd = 0;
//        int elseDel = 0;
//
//        for (String commit : commits) {
//            authors.add(getAuthor(commit));
//
//            String diff = getGitDiff(commit, filePath);
//            stmtAdded += countLinesStartingWith(diff, "+");
//            stmtDeleted += countLinesStartingWith(diff, "-");
//            condChanges += countConditionChanges(diff);
//            elseAdd += countPattern(diff, "\\+.*else");
//            elseDel += countPattern(diff, "-.*else");
//        }
//
//        method.setAuthors(authors.size());
//        method.setStmtAdded(stmtAdded);
//        method.setStmtDeleted(stmtDeleted);
//        method.setChurn(stmtAdded + stmtDeleted);
//        method.setCond(condChanges);
//        method.setElseAdded(elseAdd);
//        method.setElseDeleted(elseDel);
//    }


//    private static List<String> getCommitsForMethod(String filePath, String methodName) throws IOException, InterruptedException {
//        // Usa `git log -L` per tracciare le modifiche a un metodo (se hai la firma precisa)
//        ProcessBuilder builder = new ProcessBuilder("git", "log", "-L", methodName + ":" + filePath);
//        builder.directory(new File("path/al/repo"));
//        Process process = builder.start();
//        return readCommitsFromGitLog(process.getInputStream());
//    }

    public static List<RevCommit> filterCommitsModifyingMethod(List<RevCommit> commits,
                                                               String methodName,
                                                               String filePath,
                                                               String project) throws IOException, InterruptedException {
        List<RevCommit> result = new ArrayList<>();

        for (RevCommit commit : commits) {
            String diff = getGitDiff(commit, filePath, project);

            // Cerca il nome del metodo nella diff (es. computeMetrics(...)
            if (diff.contains(methodName + "(")) {
                result.add(commit);
            }
        }

        return result;
    }

    private static List<String> readCommitsFromGitLog(InputStream input) throws IOException {
        List<String> commits = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("commit "))
                commits.add(line.substring(7));
        }
        return commits;
    }

    private static String getAuthor(String commit, String project) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("git", "show", "-s", "--format=%an", commit);
        builder.directory(new File("repos/"+ project + "/" + project + "-clone"));
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        return reader.readLine();
    }

    private static String getGitDiff(RevCommit commit, String filePath, String project) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("git", "show", commit.getName() + ":" + filePath);
        builder.directory(new File("repos/"+ project + "/" + project + "-clone"));
        Process process = builder.start();
        return new String(process.getInputStream().readAllBytes());
    }

    private static int countLinesStartingWith(String diff, String prefix) {
        return (int) Arrays.stream(diff.split("\n"))
                .filter(line -> line.startsWith(prefix) && !line.startsWith(prefix + prefix)) // evitare "+++" o "---"
                .count();
    }

    private static int countPattern(String diff, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(diff);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private static int countConditionChanges(String diff) {
        return countPattern(diff, "(\\+|-).*\\b(if|else if|switch)\\b");
    }




    /*public static void computeChangeMetrics(MethodMetrics mm, String repoPath) throws IOException, InterruptedException {
        String methodFile = mm.getMethodPath();
        Set<String> authors = new HashSet<>();
        int methodHistories = 0;
        int stmtAdded = 0, stmtDeleted = 0, churn = 0;
        int elseAdded = 0, elseDeleted = 0, cond = 0;

        ProcessBuilder logBuilder = new ProcessBuilder(
                "git", "log", "--follow", "--pretty=format:%H|%an", "--", methodFile);
        logBuilder.directory(new File(repoPath));
        Process logProcess = logBuilder.start();

        List<String> commitLines = new BufferedReader(new InputStreamReader(logProcess.getInputStream()))
                .lines().toList();
        logProcess.waitFor();

        for (String commitLine : commitLines) {
            String[] parts = commitLine.split("\\|");
            if (parts.length < 2) continue;

            String commitHash = parts[0];
            String author = parts[1];
            authors.add(author);
            methodHistories++;

            ProcessBuilder diffBuilder = new ProcessBuilder("git", "show", commitHash, "--", methodFile);
            diffBuilder.directory(new File(repoPath));
            Process diffProcess = diffBuilder.start();

            List<String> diffLines = new BufferedReader(new InputStreamReader(diffProcess.getInputStream()))
                    .lines().toList();
            diffProcess.waitFor();

            for (String line : diffLines) {
                if (line.startsWith("+") && !line.startsWith("++") && !line.equals("+}")) {
                    stmtAdded++;
                    if (line.matches(".*\\belse\\b.*")) elseAdded++;
                    if (line.matches(".*\\b(if|while|for)\\b.*")) cond++;
                } else if (line.startsWith("-") && !line.startsWith("--") && !line.equals("-}")) {
                    stmtDeleted++;
                    if (line.matches(".*\\belse\\b.*")) elseDeleted++;
                    if (line.matches(".*\\b(if|while|for)\\b.*")) cond++;
                }
            }
        }*/

    /*public static int getMethodHistories(String methodPath) throws Exception {
        String command = "git log --follow --pretty=format:%H -- " + methodPath;
        return runCommandAndCountLines(command);
    }

    public static int getDistinctAuthors(String methodPath) throws Exception {
        String command = "git log --follow --pretty=format:%an -- " + methodPath;
        Set<String> authors = new HashSet<>();
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                authors.add(line.trim());
            }
        }
        return authors.size();
    }

    public static int getStatementsAdded(String methodPath) throws Exception {
        String command = "git log -p --follow -- " + methodPath + " | grep '^+[^+]' | wc -l";
        return runCommandAndParseInt(command);
    }

    public static int getStatementsDeleted(String methodPath) throws Exception {
        String command = "git log -p --follow -- " + methodPath + " | grep '^-[^-]' | wc -l";
        return runCommandAndParseInt(command);
    }

    public static int getChurn(String methodPath) throws Exception {
        return getStatementsAdded(methodPath) + getStatementsDeleted(methodPath);
    }

    public static int getConditionChanges(String methodPath) throws Exception {
        String command = "git log -p --follow -- " + methodPath + " | grep -E '^[-+]\\s*(if|else if|switch)' | wc -l";
        return runCommandAndParseInt(command);
    }

    public static int getElseAdded(String methodPath) throws Exception {
        String command = "git log -p --follow -- " + methodPath + " | grep '^+[^+]*else' | wc -l";
        return runCommandAndParseInt(command);
    }

    public static int getElseDeleted(String methodPath) throws Exception {
        String command = "git log -p --follow -- " + methodPath + " | grep '^-[^-]*else' | wc -l";
        return runCommandAndParseInt(command);
    }

    private static int runCommandAndCountLines(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            int count = 0;
            while (reader.readLine() != null) {
                count++;
            }
            return count;
        }
    }

    private static int runCommandAndParseInt(String command) throws Exception {
        Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();
            return output != null ? Integer.parseInt(output.trim()) : 0;
        }
    }*/

        // Imposta i valori
//        mm.setMethodHistories(methodHistories);
//        mm.setAuthors(authors.size());
//        mm.setStmtAdded(stmtAdded);
//        mm.setStmtDeleted(stmtDeleted);
//        mm.setChurn(stmtAdded + stmtDeleted);
//        mm.setElseAdded(elseAdded);
//        mm.setElseDeleted(elseDeleted);
//        mm.setCond(cond);
   // }



}

