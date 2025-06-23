package model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Ticket {

    private String ticketID;

    private LocalDate creationDate;
    private LocalDate resolutionDate;
    private Release iv;
    private Release ov;
    private Release fv;
    private List<Release> av;
    private List<RevCommit> commitList;

    public Ticket(String ticketKey, LocalDate creationDate, LocalDate resolutionDate, Release ov, Release fv, List<Release> av) {
        this.ticketID = ticketKey;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        if(av.isEmpty()){
            iv = null;
        }else{
            iv = av.getFirst();
        }
        this.ov = ov;
        this.fv = fv;
        this.av = av;
        commitList = new ArrayList<>();
    }

    public Release getIv() {
        return iv;
    }

    public void setIv(Release iv) {
        this.iv = iv;
    }

    public Release getOv() {
        return ov;
    }

    public Release getFv() {
        return fv;
    }

    public List<Release> getAv() {
        return av;
    }

    public String getTicketKey() {
        return ticketID;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void addCommit(RevCommit newCommit) {
        if(!commitList.contains(newCommit)){
            commitList.add(newCommit);
        }
    }

    public List<RevCommit> getCommitList(){
        return commitList;
    }

    public LocalDate getResolutionDate() {
        return resolutionDate;
    }

    public void addAV(Release release){
        this.av.add(release);
    }

}
