package main;

import analyzer.CKRunner;
import bugtracker.Proportion;
import com.google.gson.JsonArray;
import model.Method;
import model.MethodMetrics;
import model.Release;
import model.Ticket;
import utils.CSVExporter;
import utils.GitUtils;
import utils.MetricsCalculator;
import utils.TicketRetriever;

import java.io.File;
import java.util.*;

public class Controller {


    public void analyzeProjects(List<String> projects) throws Exception {

        for (String project : projects) {
            System.out.println("📦 Progetto: " + project);

            List<Release> selectedReleases;

            try {
                selectedReleases = GitUtils.cloneAndSelectReleasesFromJira(project);

            } catch (Exception e) {
                System.err.println("❌ Errore nel clonare o selezionare le release: " + e.getMessage());
                continue;
            }

            File centralRepo;
            try {
                centralRepo = new File("./repos/" + project.toLowerCase() + "/" + project.toLowerCase() + "-clone");
            } catch (Exception e) {
                System.err.println("❌ Errore nel recupero del clone: " + e.getMessage());
                continue;
            }

            TicketController ticketController = new TicketController();

            JsonArray jsonIssues = JiraController.getJsonTickets(project);
            List<Ticket> tickets = ticketController.obtainTickets(jsonIssues, selectedReleases);
            Proportion proportion = new Proportion();
            proportion.setProject(project);
            List<Ticket> ticketsWithAV = ticketController.returnConsistentTickets(tickets);
            for(Ticket ticket : ticketsWithAV) {
                proportion.addProportion(ticket);
            }


            List<Ticket> fixedTickets = ticketController.fixTicket(tickets, selectedReleases, proportion);
            //Map<String, Set<String>> buggyMap = JiraBugFetcher.buildBuggyMethodMap1(fixedTickets, centralRepo.getAbsolutePath());

            TicketRetriever ticketRetriever = new TicketRetriever();

            List<Method> modifiedMethods = ticketRetriever.extractModifiedMethodsFromCommits(fixedTickets, centralRepo);

            String releaseTag;

            //filtro le release prendendo il primo 34%
            List<Release> finalReleases = selectedReleases.subList(0, (int) Math.ceil(selectedReleases.size() * 0.34));


            for (Release release : finalReleases) {


                File releaseDir;
                try {
                    releaseDir = GitUtils.checkout(project.toLowerCase(), release.releaseName());
                } catch (Exception e) {
                    System.err.println("❌ Errore nel checkout della release " + release.getReleaseName() + ": " + e.getMessage());
                    continue;
                }




                List<Method> methodMetrics = CKRunner.run(project.toLowerCase(), release.getReleaseName(), releaseDir);

                for (Method m : methodMetrics) {

                    m.setReleaseId(release.getReleaseName());


                    for(Method modifiedMethod : modifiedMethods){
                        if(modifiedMethod.getMethodPath().equals(m.getMethodPath())){
//
                            m.setBuggy(true);
//
                            System.out.println("✅ [" + release.getReleaseName() + "] Metodo buggy: " + m.getMethodPath());
                        }
                    }




                    if(m.isBuggy()) {
                        m.setMethodHistories(MetricsCalculator.computeMethodModifications(m, finalReleases, releaseDir));
                    }



                }

                CSVExporter.export(project.toLowerCase(), release.getReleaseName(), methodMetrics);
            }
            CSVExporter.mergeAllCSVs(project.toLowerCase());

           // WEKAController.classify(project.toLowerCase());

        }
    }
}
