package utils;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.util.*;

public class GitUtils {

    private static final String BASE_REPO_DIR = "./repos/";
    private static final String GITHUB_BASE_URL = "https://github.com/apache/";


    public static List<String> cloneAndFilterReleases(String project) throws Exception {
        File projectDir = new File(BASE_REPO_DIR + project);
        File cloneDir = new File(projectDir, project + "-clone");
        File gitDir = new File(cloneDir, ".git");

        if (!gitDir.exists()) {
            System.out.println("📥 Clonazione iniziale in " + cloneDir.getPath());
            Git.cloneRepository()
                    .setURI(GITHUB_BASE_URL + project + ".git")
                    .setDirectory(cloneDir)
                    .call();
        } else {
            System.out.println("✅ Clone già presente: " + cloneDir.getPath());
        }

        Git git = Git.open(cloneDir);

        List<Ref> tags = git.tagList().call();
        Map<String, Date> tagDateMap = new HashMap<>();

        for (Ref tag : tags) {
            String tagName = tag.getName().replace("refs/tags/", "");
            try {
                ObjectId tagId = git.getRepository().resolve("refs/tags/" + tagName);
                RevWalk walk = new RevWalk(git.getRepository());
                RevCommit commit = walk.parseCommit(tagId);
                tagDateMap.put(tagName, commit.getCommitterIdent().getWhen());
            } catch (Exception ignored) {}
        }

        List<String> sortedTags = new ArrayList<>(tagDateMap.keySet());
        sortedTags.sort(Comparator.comparing(tagDateMap::get));

        int limit = (int) (sortedTags.size() * 0.33);
        return sortedTags.subList(0, limit);
    }



    public static File checkout(String project, String release) throws Exception {
        File projectDir = new File(BASE_REPO_DIR + project);
        File cloneDir = new File(projectDir, project + "-clone");
        File releaseDir = new File(projectDir, release);

        if (releaseDir.exists()) {
            System.out.println("✅ Release " + release + " già presente, skip checkout.");
            return releaseDir;
        }

        System.out.println("📦 Checkout della release " + release + " in " + releaseDir.getPath());

        FileUtils.copyDirectory(cloneDir, releaseDir);

        try (Git git = Git.open(releaseDir)) {
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
            git.clean().setForce(true).setCleanDirectories(true).call();
            git.checkout().setName("refs/tags/" + release).call();
        }

        return releaseDir;
    }



}
