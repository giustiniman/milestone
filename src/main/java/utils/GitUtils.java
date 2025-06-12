package utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Release;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import static main.JiraController.fetchReleasedVersionsFromJira;

public class GitUtils {

    private static final String BASE_REPO_DIR = "./repos/";
    private static final String GITHUB_BASE_URL = "https://github.com/apache/";

    public static List<Release> cloneAndSelectReleasesFromJira(String project) throws Exception {
        File projectDir = new File(BASE_REPO_DIR + project.toLowerCase());
        File cloneDir = new File(projectDir, project.toLowerCase() + "-clone");
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

        List<Release> allReleases = fetchReleasedVersionsFromJira(project);

        int limit = (int) Math.ceil(allReleases.size() * 0.34);
        List<Release> selected = allReleases.subList(0, limit);

        for  (Release release : selected) {
            System.out.println("✅ Release selezionate da JIRA (primo 34%): " + release.releaseName());

        }

        return selected;
    }

    public static File checkout(String project, String releaseTag) throws Exception {
        File projectDir = new File(BASE_REPO_DIR + project);
        File cloneDir = new File(projectDir, project + "-clone");
        File releaseDir = new File(projectDir, releaseTag);

        if (releaseDir.exists()) {
            System.out.println("✅ Release " + releaseTag + " già presente, skip checkout.");
            return releaseDir;
        }

        System.out.println("📦 Checkout della release " + releaseTag + " in " + releaseDir.getPath());

        FileUtils.copyDirectory(cloneDir, releaseDir);

        try (Git git = Git.open(releaseDir)) {
            git.reset().setMode(ResetCommand.ResetType.HARD).call();
            git.clean().setForce(true).setCleanDirectories(true).call();
            git.checkout().setName("refs/tags/" + releaseTag).call();
        }

        return releaseDir;
    }







}