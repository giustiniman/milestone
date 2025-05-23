package utils;

import model.MethodMetrics;

import java.io.*;
import java.util.List;

public class CSVExporter {

    public static void export(String project, String release, List<MethodMetrics> metrics) {
        try {
            // Crea cartella output se non esiste
            new File("output").mkdirs();

            FileWriter writer = new FileWriter("output/" + project + "_" + release + ".csv");
            writer.write("project,methodPath,releaseId,loc,cyclomaticComplexity,cognitiveComplexity,statements,nestingDepth,parameterCount,codeSmells,bugginess\n");

            for (MethodMetrics m : metrics) {
                writer.write(String.format("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%s\n",
                        m.getProject(), m.getMethodPath(), m.getReleaseId(),
                        m.getLoc(), m.getCyclomaticComplexity(), m.getCognitiveComplexity(),
                        m.getStatements(), m.getNestingDepth(), m.getParameterCount(),
                        m.getCodeSmells(), m.isBuggy() ? "yes" : "no"));
            }

            writer.close();
            System.out.println("CSV esportato: output/" + project + "_" + release + ".csv");
        } catch (IOException e) {
            System.err.println("Errore durante l'esportazione CSV: " + e.getMessage());
        }
    }

    public static void mergeAllCSVs(String project) {
        File outputDir = new File("output");
        File[] files = outputDir.listFiles((dir, name) -> name.startsWith(project + "_") && name.endsWith(".csv"));
        if (files == null || files.length == 0) return;

        try (FileWriter writer = new FileWriter("output/" + project + "_dataset.csv")) {
            boolean headerWritten = false;

            for (File f : files) {
                BufferedReader reader = new BufferedReader(new FileReader(f));
                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        if (headerWritten) continue; // salta intestazione extra
                        headerWritten = true;
                    }
                    writer.write(line + "\n");
                }
                reader.close();
            }

            System.out.println("✅ File unificato creato: output/" + project + "_dataset.csv");

        } catch (IOException e) {
            System.err.println("Errore durante l'unione dei CSV: " + e.getMessage());
        }
    }



}

