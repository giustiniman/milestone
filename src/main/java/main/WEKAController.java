package main;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;


import java.util.Random;

public class WEKAController {

    public static void classify(String project) throws Exception {

        String datasetPath = "output/" + project + "_dataset.csv";
        //String datasetPath = "./csv/final/bookkeeper.csv";

        // Carica il dataset
        DataSource source = new DataSource(datasetPath);
        Instances data = source.getDataSet();
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1); // ultima colonna = buggy/non-buggy

        evaluateClassifier("RandomForest", new RandomForest(), data);
        evaluateClassifier("NaiveBayes", new NaiveBayes(), data);
        evaluateClassifier("IBk", new IBk(), data);

    }

    private static void evaluateClassifier(String name, Classifier classifier, Instances data) throws Exception {
        System.out.println("\n🔍 Classificatore: " + name);
        Evaluation eval = new Evaluation(data);

        Random rand = new Random(1);
        int folds = 10;
        int repetitions = 10;

        for (int i = 0; i < repetitions; i++) {
            Instances randData = new Instances(data);
            randData.randomize(rand);
            if (randData.classAttribute().isNominal())
                randData.stratify(folds);

            for (int n = 0; n < folds; n++) {
                Instances train = randData.trainCV(folds, n);
                Instances test = randData.testCV(folds, n);
                Classifier clsCopy = AbstractClassifier.makeCopy(classifier);
                clsCopy.buildClassifier(train);
                eval.evaluateModel(clsCopy, test);
            }
        }

        // Metriche richieste
        System.out.printf(" - Precision: %.4f\n", eval.precision(1));
        System.out.printf(" - Recall: %.4f\n", eval.recall(1));
        System.out.printf(" - AUC: %.4f\n", eval.areaUnderROC(1));
        System.out.printf(" - Kappa: %.4f\n", eval.kappa());
        System.out.printf(" - NPofB20: %.4f\n", calculateNPofB20(eval, data));
    }

    private static double calculateNPofB20(Evaluation eval, Instances data) {
        // Placeholder: NPofB20 va calcolato come: % dei bug coperti nel 20% delle classi più sospette
        // Dovrai ordinarle per probabilità di buggy, simulare la priorizzazione e calcolare la percentuale di bug catturati.
        return -1.0; // da implementare
    }

}
