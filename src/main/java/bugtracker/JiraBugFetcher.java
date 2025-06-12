package bugtracker;

import com.google.gson.*;
import model.Release;
import model.Ticket;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JiraBugFetcher {

    private List<Ticket> tickets;





//    public static Map<String, Set<String>> buildBuggyMethodMap(JsonArray issues, String repoClonePath, List<Release> selectedReleases) {
//        Map<String, Set<String>> releaseToBuggyMethods = new HashMap<>();
//
//
//            for (JsonElement el : issues) {
//
//                JsonObject issue = el.getAsJsonObject();
//
//               // System.out.println(issue.toString());
//
//                String issueKey = issue.get("key").getAsString();
//
//                JsonObject fields = issue.getAsJsonObject("fields");
//
//               // System.out.println(issueKey + " " + fields);
//
//
//                JsonArray fixVersionsArray = fields.getAsJsonArray("fixVersions");
//
//
//                JsonArray affectedVersionsArray = fields.getAsJsonArray("versions");
//
//                String creationDateString = fields.get("created").getAsString();
//
//                String resolutionDateString = "";
//                LocalDate resolutionDate = null;
//
//                if(fields.has("resolutiondate")) {
//                    resolutionDateString = fields.get("resolutiondate").getAsString();
//                    resolutionDate = LocalDate.parse(resolutionDateString.substring(0,10));
//                    System.out.println(resolutionDate);
//                }
//                LocalDate creationDate = LocalDate.parse(creationDateString.substring(0,10));
//                //LocalDate resolutionDate = LocalDate.parse(resolutionDateString.substring(0,10));
//
//
//
//                System.out.println(creationDate);
//
//
//                List<Release> affectedVersionList = new ArrayList<>(List.of());
//                List<Release> fixedVersionList = new ArrayList<>(List.of());
//
//                Set<String> fixedSet = new HashSet<>();
//                Release fixVersion = null;
//
//                for (JsonElement a : affectedVersionsArray) {
//                    if (a.getAsJsonObject().has("name")) {
//                        LocalDate releaseDate = LocalDate.parse(a.getAsJsonObject().get("releaseDate").getAsString());
//
//                        affectedVersionList.add(new Release(a.getAsJsonObject().get("name").getAsString(), releaseDate));
//                    }
//                }
//
//                //assumo che la fix version sia la più recente
//                for (JsonElement b : fixVersionsArray) {
//                    if (b.getAsJsonObject().has("name")) {
//
//                        LocalDate releaseDate = LocalDate.parse(b.getAsJsonObject().get("releaseDate").getAsString());
//
//                        fixedVersionList.add(new Release(b.getAsJsonObject().get("name").getAsString(), releaseDate));
//
//                    }
//                }
//                if(!fixedVersionList.isEmpty()) {
//                    fixVersion = Collections.max(fixedVersionList, Comparator.comparing(Release::getReleaseDate));
//
//                    System.out.println("fixVersion più recente: " + fixVersion.releaseName());
//                }
////                String newest;
////                if(!fixedVersionList.isEmpty()) {
////                    newest = Collections.max(fixedVersionList, JiraBugFetcher::compareVersions);
////
////                }
//
//                Release openingVersion = getReleaseAfterOrEqualDate(creationDate, selectedReleases);
//
//
//
//                Ticket ticket = new Ticket(issueKey, creationDate, resolutionDate, openingVersion, fixVersion, affectedVersionList);
//
//                assert fixVersionsArray != null;
//
//                System.out.println("🐞 Bug " + issueKey + " → Affected: [" + affectedVersionList.stream()
//                        .map(Release::getReleaseName)
//                        .collect(Collectors.joining(", ")) + "] → Fixed: [" + fixedVersionList.stream()
//                        .map(Release::getReleaseName)
//                        .collect(Collectors.joining(", ")) + "] " + fixVersionsArray.size());
//
//
//                Map<String, Set<String>> modified = getModifiedMethodsFromBugCommits(issueKey, new File(repoClonePath));
//                for (String file : modified.keySet()) {
//                    for (String method : modified.get(file)) {
//                        String methodKey = file + "/" + method;
//                        for (Release affected : affectedVersionList) {
//                            releaseToBuggyMethods.putIfAbsent(affected.getReleaseName(), new HashSet<>());
//                            releaseToBuggyMethods.get(affected.getReleaseName()).add(methodKey);
//                        }
//                    }
//                }
//            }
//
//
//        return releaseToBuggyMethods;
//    }

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



//    public static int compareVersions(String v1, String v2) {
//        String[] parts1 = v1.split("\\.");
//        String[] parts2 = v2.split("\\.");
//
//        int maxLen = Math.max(parts1.length, parts2.length);
//        for (int i = 0; i < maxLen; i++) {
//            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
//            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
//            if (num1 != num2) return Integer.compare(num1, num2);
//        }
//        return 0;
//    }


//    public static Release getReleaseAfterOrEqualDate(LocalDate specificDate, List<Release> releasesList) {
//
//        //sorting the releases by their date
//        releasesList.sort(Comparator.comparing(Release::releaseDate));
//
//        //the first release which has a date after or equal to the one given is returned
//        for (Release release : releasesList) {
//            if (!release.releaseDate().isBefore(specificDate)) {
//                return release;
//            }
//        }
//        return null;
//    }

    public static Map<String, Set<String>> buildBuggyMethodMap1(List<Ticket> ticketList, String repoClonePath) {

        Map<String, Set<String>> releaseToBuggyMethods = new HashMap<>();

        for(Ticket ticket : ticketList) {

            Map<String, Set<String>> modified = getModifiedMethodsFromBugCommits(ticket.getTicketKey(), new File(repoClonePath));
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

//    public static Map<String, Set<String>> starter(String project, String repoClonePath, List<Release> selectedReleases) throws IOException {
//
//        JsonArray tickets = getTickets(project);
//        Map<String, Set<String>> releaseToBuggyMethods = buildBuggyMethodMap(tickets, repoClonePath, selectedReleases);
//
//
//        return releaseToBuggyMethods;
//    }

}

