package bugtracker;

import com.google.gson.JsonArray;
import main.JiraController;
import main.TicketController;
import model.Release;
import model.Ticket;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.max;

public class Proportion {

    private List<Float> proportionList;

    private float totalProportion;


    private enum Projects {
        AVRO,
        SYNCOPE,
        STORM,
        ZOOKEEPER
    }

    public Proportion(){
        this.proportionList = new ArrayList<>();
        this.totalProportion = 0;
    }

    public void fixTicketWithProportion(Ticket ticket, List<Release> releaseList) throws IOException, URISyntaxException {
        int estimatedIV;
        float proportion;

        System.out.println("Ticket fixato: " + ticket.getTicketKey());

        if(proportionList.size() < 5){
            proportion = coldStart(ticket.getResolutionDate());
        }else{
            proportion = increment();
        }

        estimatedIV = obtainIV(proportion, ticket);

        for(Release release : releaseList){
            if(estimatedIV == release.id()){
                ticket.setIv(release);
                ticket.addAV(release);
            }
        }

    }

    private float increment() {
        return this.totalProportion / this.proportionList.size();
    }

    private float coldStart(LocalDate resolutionDate) throws IOException, URISyntaxException {

        List<Float> proportionListTemp = new ArrayList<>();

        for(Projects project: Projects.values()){

            System.out.println(project);


           // JiraBugFetcher jiraBugFetcher = new ExtractFromJira(project.toString().toUpperCase());
          //  JiraBugFetcher jiraBugFetcher = new JiraBugFetcher();
            JsonArray issues = JiraController.getJsonTickets(String.valueOf(project));
            List<Release> allReleases = JiraController.fetchReleasedVersionsFromJira(String.valueOf(project));
            TicketController ticketController = new TicketController();
            List<Ticket> allTickets = ticketController.obtainTickets(issues, allReleases);

         //   List<Release> releaseList = jiraExtractor.getAllReleases();
          //  List<Ticket> allTickets = jiraExtractor.getAllTickets(releaseList, false);


            //need to obtain all tickets that have AV set
            List<Ticket> consistentTickets = TicketController.returnConsistentTickets(allTickets, resolutionDate);
            if(consistentTickets.size() >= 5){

                Proportion proportion = new Proportion();

                for(Ticket t: consistentTickets){
                    proportion.addProportion(t);
                }

                proportionListTemp.add(proportion.increment());
            }

        }

        return median(proportionListTemp);


    }

    private int obtainIV(float proportion, Ticket ticket){
        int ov = ticket.getOv().id();
        int fv = ticket.getFv().id();
        int estimatedIV;

        if(ov!=fv){
            estimatedIV = max(1, (int)(fv - proportion*(fv - ov)));
        }else{
            estimatedIV = max(1, (int)(fv - proportion));
        }

        return estimatedIV;
    }

    public void addProportion(Ticket ticket) {
        int denominator;
        float proportion;
        //System.out.println("ECCOMIIIII " + ticket.getTicketKey());
        int ov = ticket.getOv().id();
        int fv = ticket.getFv().id();

        if(ov == fv){
            denominator = 1;
        }else{
            denominator = fv-ov;
        }

        proportion = (float)(fv - ticket.getIv().id())/denominator;

        this.proportionList.add(proportion);
        this.totalProportion += proportion;

    }

    public static float median(List<Float> array){
        float median;

        Collections.sort(array);

        int size = array.size();
        if (size % 2 == 0) {
            median = (array.get((size / 2) - 1) + array.get(size / 2)) / 2;
        } else {
            median = array.get(size / 2);
        }

        return median;
    }

}