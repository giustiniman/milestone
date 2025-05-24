package main;

import analyzer.CKRunner;
import bugtracker.JiraBugFetcher;
import model.MethodMetrics;
import utils.CSVExporter;
import utils.GitUtils;

import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String[] projects = {"bookkeeper", "openjpa"};

        for (String project : projects) {
            System.out.println("Analizzando progetto: " + project);
            List<String> releases = GitUtils.cloneAndFilterReleases(project);

            for (String release : releases) {
                System.out.println("Analizzando release: " + release);
                File releaseDir = GitUtils.checkout(project, release);
                List<MethodMetrics> metrics = CKRunner.run(project, release, releaseDir);
                JiraBugFetcher.markBuggyMethods(project, release, metrics);
                CSVExporter.export(project, release, metrics);
            }

            CSVExporter.mergeAllCSVs(project);


        }

        System.out.println("Analisi completata.");
    }
}
