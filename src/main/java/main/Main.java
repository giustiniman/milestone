package main;

import analyzer.CKRunner;
import bugtracker.JiraBugFetcher;
import model.MethodMetrics;
import utils.CSVExporter;
import utils.GitUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        List<String> projects = Arrays.asList("bookkeeper");

        for (String project : projects) {
            System.out.println("📦 Progetto: " + project);

            List<String> releases = GitUtils.cloneAndFilterReleases(project);
            File centralRepo = new File("repos/" + project + "/bookkeeper-clone");

            Map<String, Set<String>> buggyMap = JiraBugFetcher.buildBuggyMethodMap(project, centralRepo.getAbsolutePath());

            for (String release : releases) {
                File releaseDir = GitUtils.checkout(project, release);
                List<MethodMetrics> metrics = CKRunner.run(project, release, releaseDir);

                for (MethodMetrics m : metrics) {
                    String path = m.getMethodPath().split("\\(")[0].replace("\\\\", "/");
                    String[] parts = path.split("/");
                    String methodName = parts[parts.length - 1].replaceAll("\\[.*]", "");
                    String methodPath = String.join("/", Arrays.copyOf(parts, parts.length - 1));
                    String fullKey = methodPath + "/" + methodName;
                    String normRelease = release.replace("release-", "");

                    if (buggyMap.containsKey(normRelease) && buggyMap.get(normRelease).contains(fullKey)) {
                        m.setBuggy(true);
                        System.out.println("✅ [" + release + "] Metodo buggy: " + fullKey);
                    }
                }

                CSVExporter.export(project, release, metrics);
            }
            CSVExporter.mergeAllCSVs(project);
        }
        System.out.println("Analisi completata.");
    }
}