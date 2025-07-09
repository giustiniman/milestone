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

    public List<Ticket> returnConsistentTickets(List<Ticket> allTickets) {

        List<Ticket> ticketsWithAV = new ArrayList<>();

        for (Ticket ticket : allTickets) {

            if(!ticket.getAv().isEmpty()){
                ticketsWithAV.add(ticket);
            }

        }

        return ticketsWithAV;
    }

    public List<Ticket> fixTicket(List<Ticket> tickets, List<Release> selectedReleases, Proportion proportion) throws IOException, URISyntaxException {

        List<Ticket> finalTicketsList = new ArrayList<>();


        //Proportion proportion = new Proportion();
        for (Ticket ticket : tickets) {
            if(ticket.getAv().isEmpty()){
                proportion.fixTicketWithProportion(ticket, selectedReleases);
                completeAV(ticket, selectedReleases);
                System.out.println(ticket.getTicketKey() + " [" + ticket.getAv().stream()
                        .map(Release::getReleaseName)
                        .collect(Collectors.joining(", ")) + "]");
            }
            finalTicketsList.add(ticket);
        }


        return finalTicketsList;
    }

    public static void completeAV(Ticket ticket, List<Release> releases){

       // System.out.println(ticket.getIv());

        int iv = ticket.getIv().id();
        int fv = ticket.getFv().id();

        System.out.println("Ticket Key " + ticket.getTicketKey() + " IV: " + iv +  " FV: " + fv);

        for(Release release : releases){
            if(release.id() > iv && release.id() < fv ){
                //System.out.println("ciaooo");
                ticket.addAV(release);
            }
        }

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

                        //fixedVersionList.add(selectedReleases.stream().filter(release -> release.getReleaseDate().equals(releaseDate)).findFirst().get());

                        //fixedVersionList.add(new Release(b.getAsJsonObject().get("name").getAsString(), releaseDate));

                        selectedReleases.stream()
                                .filter(release -> release.getReleaseDate().equals(releaseDate))
                                .findFirst()
                                .ifPresent(fixedVersionList::add);

                    }
                }
            }

            if(!fixedVersionList.isEmpty()) {
                fixVersion = Collections.max(fixedVersionList, Comparator.comparing(Release::getReleaseDate));

                //System.out.println("fixVersion più recente: " + fixVersion.releaseName());
            }
            else if(resolutionDate != null) {
                fixVersion = getReleaseAfterOrEqualDate(resolutionDate, selectedReleases);
            }

            Release openingVersion = getReleaseAfterOrEqualDate(creationDate, selectedReleases);

            if(openingVersion != null && fixVersion != null) {


                Ticket ticket = new Ticket(issueKey, creationDate, resolutionDate, openingVersion, fixVersion, affectedVersionList);

                if(issueKey.contains("BOOKKEEPER")) {

                    System.out.println("🐞 Bug " + issueKey + " " + openingVersion.getReleaseName() + " → Affected: [" + affectedVersionList.stream()
                            .map(Release::getReleaseName)
                            .collect(Collectors.joining(", ")) + "] → Fixed: [" + fixVersion.getReleaseName() + "] " + fixVersionsArray.size());

                }

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
            if (!release.releaseDate().isBefore(specificDate)) {
                 return release;
            }
        }
        return null;
    }
}
