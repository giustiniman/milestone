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
import java.util.List;

public class JiraController {

    private static final String BASE_URL = "https://issues.apache.org/jira/rest/api/2/search";


//    public static List<Release> fetchReleasedVersionsFromJira(String projectKey) {
//        List<Release> releases = new ArrayList<>();
//        try {
//            String apiUrl = "https://issues.apache.org/jira/rest/api/2/project/" + projectKey + "/versions";
//            HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
//            conn.setRequestMethod("GET");
//            conn.setRequestProperty("Accept", "application/json");
//
//            InputStreamReader reader = new InputStreamReader(conn.getInputStream());
//            JsonArray versionArray = JsonParser.parseReader(reader).getAsJsonArray();
//
//            System.out.println(versionArray.toString());
//
//            for (JsonElement element : versionArray) {
//                JsonObject obj = element.getAsJsonObject();
//                System.out.println(obj.toString());
//                //System.out.println(obj.get("name").getAsString());
//                if (obj.has("name")) {
//                    String name = obj.get("name").getAsString();
//                    //System.out.println(name);
//                    //String releaseDateString = obj.get("releaseDate").getAsString();
//                    LocalDate releaseDate = LocalDate.parse(obj.get("releaseDate").getAsString());
//                    //System.out.println(releaseDate);
//                    if (name.matches("\\d+\\.\\d+\\.\\d+")) {
//
//                        releases.add(new Release(name, releaseDate));
//                    }
//                }
//            }
//            for  (Release release : releases) {
//               System.out.println(release.getReleaseName()  );
//            }
//        } catch (Exception e) {
//            System.err.println("❌ Errore nel recupero versioni da JIRA: " + e.getMessage());
//        }
//
//        return releases;
//    }

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

            if (obj.has("name") && obj.has("released") && obj.get("released").getAsBoolean()) {
                String name = obj.get("name").getAsString();
              //  System.out.println(name);

                if (name.matches("\\d+\\.\\d+\\.\\d+")) {
                    try {
                        LocalDate releaseDate;
                        if (obj.has("releaseDate")) {
                          //  System.out.println("ciaoooo");
                            releaseDate = LocalDate.parse(obj.get("releaseDate").getAsString());
                            releases.add(new Release(name, releaseDate));
                        }


                    } catch (Exception e) {
                        System.err.println("⚠️ Errore parsing releaseDate per " + name + ": " + e.getMessage());
                    }
                }
            }
        }


//        for (Release release : releases) {
//            System.out.println("✅ Versione rilasciata: " + release.getReleaseName());
//        }

    } catch (Exception e) {
        System.err.println("❌ Errore nel recupero versioni da JIRA: " + e.getMessage());
    }

    return releases;
}


    public static JsonArray getJsonTickets(String project) throws IOException {

        String jql = String.format(
                "project=%s AND issuetype=Bug AND (status=Closed OR status=Resolved) AND resolution=Fixed AND affectedVersion in (EMPTY, releasedVersions()) AND fixVersion in releasedVersions()",
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
