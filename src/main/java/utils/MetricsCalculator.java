package utils;

import model.Method;
import model.Release;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;

import java.io.File;
import java.util.List;

public class MetricsCalculator {

    public static int computeMethodModifications(Method targetMethod, List<Release> releases, File repoDir) {
        int modificationCount = 0;

        TicketRetriever ticketRetriever = new TicketRetriever();

        try (Git git = Git.open(repoDir)) {
            Repository repository = git.getRepository();

            for (Release release : releases) {

                //System.out.println(release.getReleaseName());

                // Recupera i commit della release
                List<RevCommit> commits = GitUtils.getCommitsForRelease(repoDir.getAbsolutePath(), release.releaseName());

                for (RevCommit commit : commits) {
                    RevCommit parent = commit.getParentCount() > 0 ? commit.getParent(0) : null;

                    AbstractTreeIterator oldTreeParser = ticketRetriever.prepareTreeParser(repository, parent);
                    AbstractTreeIterator newTreeParser = ticketRetriever.prepareTreeParser(repository, commit);

                    List<DiffEntry> diffs = git.diff()
                            .setOldTree(oldTreeParser)
                            .setNewTree(newTreeParser)
                            .call();

                    for (DiffEntry diff : diffs) {
                        String newPath = diff.getNewPath();
                        if (newPath.endsWith(".java")) {
                            String sourceCode = ticketRetriever.readFileFromGit(repository, commit, newPath);

                            List<Method> modifiedMethods = ticketRetriever.extractMethodsFromSource(sourceCode, newPath);

                            for (Method modified : modifiedMethods) {
                                if (modified.getMethodPath().equals(targetMethod.getMethodPath())) {
                                    modificationCount++;
                                    break; // Basta una volta per commit
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Errore durante il calcolo delle modifiche: " + e.getMessage());
        }

        return modificationCount;
    }



}
