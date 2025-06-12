package model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class Release {
    private int id;
    //version name
    private String releaseName;
    //version date
    private LocalDate releaseDate;
    //list of all commits related to that version
    private List<RevCommit> commitList;
    //list of all classes related to that version
    //private List <JavaClass> classes;

    public Release(String releaseName, LocalDate releaseDate) {
        this.releaseName = releaseName;
        this.releaseDate = releaseDate;
        commitList = new ArrayList<>();
        //classes = new ArrayList<>();
    }

//    public Release(int id, String releaseName, LocalDate releaseDate) {
//        this.id = id;
//        this.releaseName = releaseName;
//        this.releaseDate = releaseDate;
//        commitList = new ArrayList<>();
//        //classes = new ArrayList<>();
//    }

    public int id() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String releaseName() {
        return releaseName;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public LocalDate releaseDate() {
        return releaseDate;
    }

    public String getReleaseName() {
        return releaseName;
    }
//    public List<JavaClass> getClasses() {
//        return classes;
//    }

//    public void setClasses(List<JavaClass> classes) {
//        this.classes = classes;
//    }

//    public void addClass(JavaClass newClass){
//        this.classes.add(newClass);
//    }

    public List<RevCommit> getCommitList() {
        return commitList;
    }

    public void addCommit(RevCommit commit){
        this.commitList.add(commit);
    }


}
