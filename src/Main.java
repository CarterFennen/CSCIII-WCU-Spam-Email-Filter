/**
 * @author Carter Fennen
 * @date April 2026
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/*
 * Main - entry point for the spam filter program.
 * Orchestrates the full pipeline from reading the dataset
 * to training both classifiers and evaluating their accuracy.
 *
 * Pipeline: Load Emails > Shuffle and Split > Extract Features > Train > Evaluate > Write Output
 */
public class Main {
    public static void main(String[] args) {

        /*
         * Step 1: Load Emails
         * CSVParser reads the dataset and converts each row into an
         * Email object containing the id, raw text, and label.
         */
        CSVParser parser = new CSVParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        System.out.println("Loaded: " + emails.size() + " emails\n");

        /*
         * Step 2: Shuffle and Split
         * Shuffle with a fixed seed so results are consistent across runs.
         * Split 80% for training and 20% for testing so both sets contain
         * a representative mix of spam and ham.
         */
        Collections.shuffle(emails, new Random(42));
        int splitIndex          = (int)(emails.size() * 0.80);
        List<Email> trainEmails = emails.subList(0, splitIndex);
        List<Email> testEmails  = emails.subList(splitIndex, emails.size());

        System.out.println("Training emails: " + trainEmails.size());
        System.out.println("Testing emails:  " + testEmails.size());

        /*
         * Step 3: Find Top Spam Words
         * Only uses training data to avoid data leakage.
         * Calculates spam/ham ratio for every word and prints the top 10
         * most differentiating words after filtering out noise.
         */
        TextProcessor tp = new TextProcessor();
        System.out.println("\n--- Top 10 Spam Words (Training Set Only) ---");
        tp.findTopWords(trainEmails, 10);

        /*
         * Step 4: Extract Features
         * TextProcessor converts each email's raw text into 11 numerical
         * features based on word counts. Training emails are separated into
         * spam and ham lists. Test emails are extracted separately and kept
         * unseen until accuracy evaluation.
         */
        List<EmailFeatures> spamFeatures = new ArrayList<>();
        List<EmailFeatures> hamFeatures  = new ArrayList<>();
        List<EmailFeatures> testFeatures = new ArrayList<>();

        for (Email e : trainEmails) {
            EmailFeatures ef = tp.extractFeatures(e);
            if (e.getLabel() == 1) spamFeatures.add(ef); // label 1 = spam
            else hamFeatures.add(ef);                     // label 0 = ham
        }

        for (Email e : testEmails) {
            testFeatures.add(tp.extractFeatures(e));
        }

        System.out.println("\nTraining spam: " + spamFeatures.size());
        System.out.println("Training ham:  " + hamFeatures.size());

        /*
         * Step 5: Weighted Centroid Classifier
         * Classifies test emails by measuring weighted Euclidean distance
         * to the average spam and ham model. Uses a bias factor of 1.8
         * to compensate for the dataset imbalance of 5x more ham than spam.
         */
        System.out.println("\n--- Weighted Centroid Classifier ---");
        SpamClassifier centroid = new SpamClassifier();
        centroid.train(spamFeatures, hamFeatures);
        centroid.testAccuracy(testFeatures, testEmails);

        /*
         * Step 6: Naive Bayes Classifier
         * Classifies test emails using probability scoring instead of distance.
         * Naturally handles dataset imbalance through prior probabilities
         * without needing a manual bias factor.
         */
        System.out.println("\n--- Naive Bayes Classifier ---");
        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(spamFeatures, hamFeatures);
        nb.testAccuracy(testFeatures, testEmails);
        nb.predictAndWrite(testFeatures, "predictions.txt");

        /*
         * Step 7: Write Output Files
         * CsvWriter saves email features and summary statistics to CSV files
         * for analysis and record keeping. Run after all classifiers so the
         * files always reflect the most current run.
         */
        System.out.println("\n--- Writing Output Files ---");
        FeatureSummary spamSummary = new FeatureSummary();
        FeatureSummary hamSummary  = new FeatureSummary();
        spamSummary.summarize(spamFeatures);
        hamSummary.summarize(hamFeatures);

        List<EmailFeatures> allFeatures = new ArrayList<>();
        allFeatures.addAll(spamFeatures);
        allFeatures.addAll(testFeatures);

        CsvWriter writer = new CsvWriter();
        writer.writeFeatures(allFeatures, "email_features.csv");
        writer.writeSummary(spamSummary,  "spam_summary.csv");
        writer.writeSummary(hamSummary,   "ham_summary.csv");
    }
}
