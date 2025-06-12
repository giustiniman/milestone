package main;

import bugtracker.Proportion;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import model.Release;
import model.Ticket;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class TicketController {

    private List<Ticket> ticketList;

    public TicketController() {
        this.ticketList = new ArrayList<>();
    }

    public static List<Ticket> returnConsistentTickets(List<Ticket> allTickets, LocalDate resolutionDate) {

        List<Ticket> ticketsWithAV = new ArrayList<>();

        for (Ticket ticket : allTickets) {

            if(!ticket.getAv().isEmpty()){
                ticketsWithAV.add(ticket);
            }

        }

        return ticketsWithAV;
    }

    public List<Ticket> fixTicket(List<Ticket> tickets, List<Release> selectedReleases) throws IOException, URISyntaxException {

        for (Ticket ticket : tickets) {
            if(ticket.getAv().isEmpty()){
                Proportion proportion = new Proportion();
                proportion.fixTicketWithProportion(ticket, selectedReleases);
            }
        }


        return tickets;
    }

    public List<Ticket> obtainTickets(JsonArray jsonIssues, List<Release> selectedReleases) {

        for(JsonElement jsonElement : jsonIssues){



            JsonObject issue = jsonElement.getAsJsonObject();

            //System.out.println(issue.toString());

            String issueKey = issue.get("key").getAsString();

            JsonObject fields = issue.getAsJsonObject("fields");

            // System.out.println(issueKey + " " + fields);


            JsonArray fixVersionsArray = fields.getAsJsonArray("fixVersions");


            JsonArray affectedVersionsArray = fields.getAsJsonArray("versions");

            String creationDateString = fields.get("created").getAsString();

            String resolutionDateString;
            LocalDate resolutionDate = null;

            if(fields.has("resolutiondate")) {
                resolutionDateString = fields.get("resolutiondate").getAsString();
                resolutionDate = LocalDate.parse(resolutionDateString.substring(0,10));
                System.out.println(resolutionDate);
            }
            LocalDate creationDate = LocalDate.parse(creationDateString.substring(0,10));

           // Release openingVersion = getReleaseAfterOrEqualDate(creationDate, selectedReleases, issueKey);

           // selectedReleases.sort(Comparator.comparing(Release::releaseDate));


           // if(creationDate.isAfter(selectedReleases.getLast().getReleaseDate())) continue;
            //LocalDate resolutionDate = LocalDate.parse(resolutionDateString.substring(0,10));



           // System.out.println(creationDate);


            List<Release> affectedVersionList = new ArrayList<>(List.of());
            List<Release> fixedVersionList = new ArrayList<>(List.of());


            Release fixVersion = null;

            for (JsonElement a : affectedVersionsArray) {
                if (a.getAsJsonObject().has("name")) {
                    if(a.getAsJsonObject().has("releaseDate")) {

                        LocalDate releaseDate = LocalDate.parse(a.getAsJsonObject().get("releaseDate").getAsString());

                        affectedVersionList.add(new Release(a.getAsJsonObject().get("name").getAsString(), releaseDate));
                    }
                }
            }

            //assumo che la fix version sia la più recente
            for (JsonElement b : fixVersionsArray) {
                if (b.getAsJsonObject().has("name")) {
                    if(b.getAsJsonObject().has("releaseDate")) {
                        LocalDate releaseDate = LocalDate.parse(b.getAsJsonObject().get("releaseDate").getAsString());

                        fixedVersionList.add(new Release(b.getAsJsonObject().get("name").getAsString(), releaseDate));
                    }


                }
            }

            if(!fixedVersionList.isEmpty()) {
                fixVersion = Collections.max(fixedVersionList, Comparator.comparing(Release::getReleaseDate));

                System.out.println("fixVersion più recente: " + fixVersion.releaseName());
            }
            else if(resolutionDate != null) {
                fixVersion = getReleaseAfterOrEqualDate(resolutionDate, selectedReleases);
            }
//                String newest;
//                if(!fixedVersionList.isEmpty()) {
//                    newest = Collections.max(fixedVersionList, JiraBugFetcher::compareVersions);
//
//                }
          //  System.out.println("1"+creationDate);
            Release openingVersion = getReleaseAfterOrEqualDate(creationDate, selectedReleases);
          //  System.out.println("2"+openingVersion.getReleaseDate());
         //   if(!affectedVersionList.isEmpty() && !fixVersion) {}

            if(openingVersion != null && fixVersion != null) {


                Ticket ticket = new Ticket(issueKey, creationDate, resolutionDate, openingVersion, fixVersion, affectedVersionList);

                System.out.println("🐞 Bug " + issueKey + " " + openingVersion.getReleaseName() + " → Affected: [" + affectedVersionList.stream()
                        .map(Release::getReleaseName)
                        .collect(Collectors.joining(", ")) + "] → Fixed: [" + fixedVersionList.stream()
                        .map(Release::getReleaseName)
                        .collect(Collectors.joining(", ")) + "] " + fixVersionsArray.size());


                ticketList.add(ticket);

            }

        }
        return ticketList;
    }

    public static Release getReleaseAfterOrEqualDate(LocalDate specificDate, List<Release> releasesList) {

        //sorting the releases by their date
        releasesList.sort(Comparator.comparing(Release::releaseDate));

        //the first release which has a date after or equal to the one given is returned
        for (Release release : releasesList) {
//            if (issueKey.equals("AVRO-4126")) {
//            System.out.println(specificDate+"aoooo "+ release.getReleaseDate());
//            }
            if (!release.releaseDate().isBefore(specificDate)) {
              //  System.out.println("fjindibnj");
                return release;
            }
        }
        return null;
    }
}
