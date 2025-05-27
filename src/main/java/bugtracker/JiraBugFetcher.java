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

    public static void markBuggyMethods(String project, String currentRelease, List<MethodMetrics> metrics, List<String> allReleases) {

        String normalizedRelease = currentRelease.replace("release-", "");
        System.out.println("🔍 Analizzando release: " + normalizedRelease);

        try {
            String projectKey = getJiraProjectKey(project);
            if (projectKey == null) return;

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

            Map<String, Set<String>> releaseToBuggyMethods = new HashMap<>();

            for (JsonElement el : issues) {
                JsonObject issue = el.getAsJsonObject();
                String issueKey = issue.get("key").getAsString();

                JsonArray fixVersions = issue.getAsJsonObject("fields").getAsJsonArray("fixVersions");
                JsonArray affectedVersions = issue.getAsJsonObject("fields").getAsJsonArray("versions");

                Set<String> affectedSet = new HashSet<>();
                if (affectedVersions != null) {
                    for (JsonElement a : affectedVersions) {
                        if (a.getAsJsonObject().has("name")) {
                            String name = a.getAsJsonObject().get("name").getAsString();
                            affectedSet.add(name);
                        }
                    }
                }

                System.out.println("🐞 Bug " + issueKey + " → Affected: " + affectedSet);

                Map<String, Set<String>> modified = getModifiedMethodsFromBugCommits(issueKey, project, currentRelease);
                for (String file : modified.keySet()) {
                    for (String method : modified.get(file)) {
                        System.out.println("📄 Modificato: " + file + " → metodo: " + method);
                        for (String affected : affectedSet) {
                            releaseToBuggyMethods.putIfAbsent(affected, new HashSet<>());
                            String methodKey = file + "/" + method;
                            releaseToBuggyMethods.get(affected).add(methodKey);
                            System.out.println("📝 Aggiunto metodo buggy in release " + affected + ": " + methodKey);
                        }
                    }
                }
            }

            for (MethodMetrics m : metrics) {
                String fullPath = m.getMethodPath().split("\\(")[0].replace("\\", "/");
                String[] parts = fullPath.split("/");
                String methodName = parts[parts.length - 1];
                String methodPath = String.join("/", Arrays.copyOf(parts, parts.length - 1));
                String fullKey = methodPath + "/" + methodName;

                if (releaseToBuggyMethods.containsKey(normalizedRelease)) {
                    if (releaseToBuggyMethods.get(normalizedRelease).contains(fullKey)) {
                        m.setBuggy(true);
                        System.out.println("✅ Metodo marcato buggy in " + normalizedRelease + ": " + fullKey);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Errore interrogando JIRA: " + e.getMessage());
        }
    }

    private static Map<String, Set<String>> getModifiedMethodsFromBugCommits(String issueKey, String project, String release) {
        Map<String, Set<String>> result = new HashMap<>();
        String repoPath = "./repos/" + project + "/" + release;

        try {
            Process logProc = new ProcessBuilder("git", "log", "--pretty=format:%H", "--grep=" + issueKey)
                    .directory(new File(repoPath))
                    .start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(logProc.getInputStream()));
            String commitHash;
            while ((commitHash = reader.readLine()) != null) {
                System.out.println("🔍 Commit trovato per " + issueKey + ": " + commitHash);

                Process showProc = new ProcessBuilder("git", "show", "--unified=0", commitHash)
                        .directory(new File(repoPath))
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
            System.err.println("❌ Errore nei comandi git: " + e.getMessage());
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
