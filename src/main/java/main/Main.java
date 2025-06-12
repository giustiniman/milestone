package main;

import analyzer.CKRunner;
import bugtracker.JiraBugFetcher;
import model.Release;
import utils.CSVExporter;
import model.MethodMetrics;
import utils.GitUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {
        List<String> projects = List.of("BOOKKEEPER");

        Controller controller =  new Controller();
        controller.analyzeProjects(projects);


        System.out.println("Analisi completata.");
    }
}
