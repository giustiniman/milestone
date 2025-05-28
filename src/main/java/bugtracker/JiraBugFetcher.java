package bugtracker;

import com.google.gson.*;
import model.MethodMetrics;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraBugFetcher {

    private static final String BASE_URL = "https://issues.apache.org/jira/rest/api/2/search";

    public static Map<String, Set<String>> buildBuggyMethodMap(String project, String repoClonePath) {
        Map<String, Set<String>> releaseToBuggyMethods = new HashMap<>();

        try {
            String projectKey = getJiraProjectKey(project);
            if (projectKey == null) return releaseToBuggyMethods;

            String jql = String.format(
                    "project=%s AND issuetype=Bug AND (status=Closed OR status=Resolved) AND resolution=Fixed",
                    projectKey
            );
            String encodedJql = java.net.URLEncoder.encode(jql, "UTF-8");
            String apiUrl = BASE_URL + "?jql=" + encodedJql + "&fields=key,fixVersions,versions&maxResults=1000";

            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray issues = json.getAsJsonArray("issues");

            for (JsonElement el : issues) {
                JsonObject issue = el.getAsJsonObject();
                String issueKey = issue.get("key").getAsString();

                JsonElement versionsElement = issue.getAsJsonObject().get("fields").getAsJsonObject().get("versions");
                if (versionsElement == null || !versionsElement.isJsonArray()) continue;

                JsonArray affectedVersions = versionsElement.getAsJsonArray();
                Set<String> affectedSet = new HashSet<>();
                if (affectedVersions != null) {
                    for (JsonElement a : affectedVersions) {
                        if (a.getAsJsonObject().has("name")) {
                            affectedSet.add(a.getAsJsonObject().get("name").getAsString());
                        }
                    }
                }

                Map<String, Set<String>> modified = getModifiedMethodsFromBugCommits(issueKey, new File(repoClonePath));
                for (String file : modified.keySet()) {
                    for (String method : modified.get(file)) {
                        String methodKey = file + "/" + method;
                        for (String affected : affectedSet) {
                            releaseToBuggyMethods.putIfAbsent(affected, new HashSet<>());
                            releaseToBuggyMethods.get(affected).add(methodKey);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Errore interrogando JIRA: " + e.getMessage());
        }

        return releaseToBuggyMethods;
    }

    private static Map<String, Set<String>> getModifiedMethodsFromBugCommits(String issueKey, File repoDir) {
        Map<String, Set<String>> result = new HashMap<>();

        try {
            Process logProc = new ProcessBuilder("git", "log", "--pretty=format:%H", "--grep=" + issueKey)
                    .directory(repoDir)
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(logProc.getInputStream()));
            String commitHash;
            while ((commitHash = reader.readLine()) != null) {
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

        return result;
    }

    private static String getJiraProjectKey(String project) {
        return switch (project.toLowerCase()) {
            case "bookkeeper" -> "BOOKKEEPER";
            case "openjpa" -> "OPENJPA";
            default -> null;
        };
    }
}
