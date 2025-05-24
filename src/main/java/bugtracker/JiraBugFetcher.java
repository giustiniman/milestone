package bugtracker;

import com.google.gson.*;
import model.MethodMetrics;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class JiraBugFetcher {

    private static final String BASE_URL = "https://issues.apache.org/jira/rest/api/2/search";

    public static void markBuggyMethods(String project, String release, List<MethodMetrics> metrics) {
        try {
            String projectKey = getJiraProjectKey(project);
            if (projectKey == null) return;

            String jql = String.format(
                    "project=%s AND issuetype=Bug AND (status=Closed OR status=Resolved) AND resolution=Fixed",
                    projectKey
            );
            String encodedJql = java.net.URLEncoder.encode(jql, "UTF-8");
            String apiUrl = BASE_URL + "?jql=" + encodedJql + "&fields=key&maxResults=1000";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray issues = json.getAsJsonArray("issues");

            Set<String> buggyFiles = new HashSet<>();

            for (JsonElement el : issues) {
                String issueKey = el.getAsJsonObject().get("key").getAsString();
                List<String> files = getFilesFromBugCommits(issueKey, project, release);
                buggyFiles.addAll(files);
            }

            // marca i metodi solo se il loro file è stato toccato da commit buggy
            for (MethodMetrics m : metrics) {
                String methodPath = m.getMethodPath().split("\\(")[0]; // rimuove il metodo tra parentesi
                methodPath = methodPath.replace("\\", "/"); // uniforma path

                // Rimuove il prefisso "release-..." se presente
                if (methodPath.matches("^release-[^/]+/.*")) {
                    methodPath = methodPath.substring(methodPath.indexOf("/") + 1);
                }

                // Troncamento a solo file.java
                methodPath = methodPath.split("\\.java")[0] + ".java";
                // tronca dopo il file

                int i=0;
                for (String buggyFile : buggyFiles) {
                    System.out.println(i+ " mp "+methodPath  + ":" + "bf " + buggyFile);
                    i++;
                    if (methodPath.equalsIgnoreCase(buggyFile)) {
                        m.setBuggy(true);
                        break;
                    }
                }
            }



            System.out.println("Bug associati a " + buggyFiles.size() + " file.");

        } catch (Exception e) {
            System.err.println("Errore interrogando JIRA: " + e.getMessage());
        }
    }

    private static List<String> getFilesFromBugCommits(String issueKey, String project, String release) {
        List<String> files = new ArrayList<>();
        String repoPath = "./repos/" + project + "/" + release;

        try {
            Process logProc = new ProcessBuilder("git", "log", "--pretty=format:%H", "--grep=" + issueKey)
                    .directory(new File(repoPath))
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(logProc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Process diffProc = new ProcessBuilder("git", "diff-tree", "--no-commit-id", "--name-only", "-r", line.trim())
                        .directory(new File(repoPath))
                        .start();

                BufferedReader diffReader = new BufferedReader(new InputStreamReader(diffProc.getInputStream()));
                String fileLine;
                while ((fileLine = diffReader.readLine()) != null) {
                    if (fileLine.endsWith(".java")) {
                        files.add(fileLine.replace("\\", "/"));  // uniforma per confronto
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Errore nei comandi git: " + e.getMessage());
        }

        return files;
    }


    private static String getJiraProjectKey(String project) {
        return switch (project.toLowerCase()) {
            case "bookkeeper" -> "BOOKKEEPER";
            case "openjpa" -> "OPENJPA";
            default -> null;
        };
    }
}
