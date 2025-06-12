package main;

import analyzer.CKRunner;
import bugtracker.JiraBugFetcher;
import com.google.gson.JsonArray;
import model.MethodMetrics;
import model.Release;
import model.Ticket;
import utils.CSVExporter;
import utils.GitUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
            List<Ticket> fixedTickets = ticketController.fixTicket(tickets, selectedReleases);
            Map<String, Set<String>> buggyMap = JiraBugFetcher.buildBuggyMethodMap1(fixedTickets, centralRepo.getAbsolutePath());

           // Map<String, Set<String>> buggyMap = JiraBugFetcher.starter(project, centralRepo.getAbsolutePath(), selectedReleases);

            for (Release release : selectedReleases) {
                String releaseTag = "release-" + release.releaseName();
                File releaseDir;
                try {
                    releaseDir = GitUtils.checkout(project.toLowerCase(), releaseTag);
                } catch (Exception e) {
                    System.err.println("❌ Errore nel checkout della release " + releaseTag + ": " + e.getMessage());
                    continue;
                }

                List<MethodMetrics> metrics = CKRunner.run(project.toLowerCase(), releaseTag, releaseDir);

                for (MethodMetrics m : metrics) {
                    String path = m.getMethodPath().split("\\(")[0].replace("\\", "/");
                    String[] parts = path.split("/");
                    String methodName = parts[parts.length - 1].replaceAll("\\[.*]", "");
                    String methodPath = String.join("/", Arrays.copyOf(parts, parts.length - 1));
                    String fullKey = methodPath + "/" + methodName;

                    if (buggyMap.containsKey(release.getReleaseName()) && buggyMap.get(release.getReleaseName()).contains(fullKey)) {
                        m.setBuggy(true);
                        System.out.println("✅ [release-" + release.getReleaseName() + "] Metodo buggy: " + fullKey);
                    }
                }

                CSVExporter.export(project.toLowerCase(), releaseTag, metrics);
            }
            CSVExporter.mergeAllCSVs(project.toLowerCase());

            WEKAController.classify(project.toLowerCase());

        }
    }
}
