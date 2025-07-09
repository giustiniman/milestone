package main;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Release;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class JiraController {

    private static final String BASE_URL = "https://issues.apache.org/jira/rest/api/2/search";


    public static List<Release> fetchReleasedVersionsFromJira(String projectKey) {
        List<Release> releases = new ArrayList<>();
        try {
            String apiUrl = "https://issues.apache.org/jira/rest/api/2/project/" + projectKey + "/versions";
            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
            JsonArray versionArray = JsonParser.parseReader(reader).getAsJsonArray();

            for (JsonElement element : versionArray) {
                JsonObject obj = element.getAsJsonObject();
                //System.out.println("🔍 DEBUG JSON: " + obj);

                //prendo anche le unreleased
                if (obj.has("name") && obj.has("released")) {
                    String name = obj.get("name").getAsString();
                    if (name.matches("\\d+\\.\\d+\\.\\d+")) {
                        try {
                            LocalDate releaseDate;
                            if (obj.has("releaseDate")) {
                              releaseDate = LocalDate.parse(obj.get("releaseDate").getAsString());
                              String releaseName = projectKey.equals("BOOKKEEPER") ? "release-" + name : name;
                              releases.add(new Release(releaseName, releaseDate));
                            }


                        } catch (Exception e) {
                            System.err.println("⚠️ Errore parsing releaseDate per " + name + ": " + e.getMessage());
                        }
                    }
                }
            }
            releases.sort(Comparator.comparing(Release::releaseDate));
            int i = 0;
            for (Release release : releases) {
                release.setId(++i);
                System.out.println(release.getReleaseName()+" "+release.id());
            }


    } catch (Exception e) {
        System.err.println("❌ Errore nel recupero versioni da JIRA: " + e.getMessage());
    }

    return releases;
}


    public static JsonArray getJsonTickets(String project) throws IOException {

        String jql = String.format(
                "project=%s AND issuetype=Bug AND (status=Closed OR status=Resolved) AND resolution=Fixed",
                project
        );
        String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);
        String apiUrl = BASE_URL + "?jql=" + encodedJql + "&fields=key,fixVersions,versions,created&maxResults=1000";

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        JsonArray issues = json.getAsJsonArray("issues");

        return issues;

    }

}
