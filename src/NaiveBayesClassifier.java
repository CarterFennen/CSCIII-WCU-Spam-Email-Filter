/**
 * @author Carter Fennen
 * @date April 2026
 */

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * NaiveBayesClassifier - classifies emails using probability scoring
 * rather than distance measurement.
 *
 * Trains on spam and ham EmailFeatures to learn the average feature
 * values for each class and the prior probability of each class.
 * Predicts by computing a spam score and ham score for each email
 * and returning whichever class scores higher.
 *
 * Design Decision: Log probabilities are used instead of raw probabilities
 * because multiplying many small decimal numbers together creates values
 * so tiny that computers lose precision and round them to zero. Taking
 * the log converts multiplication into addition which is mathematically
 * equivalent but stable.
 */
public class NaiveBayesClassifier {

    private Map<String, Double> spamProbabilities = new HashMap<>();
    private Map<String, Double> hamProbabilities  = new HashMap<>();
    private double spamPrior; // baseline probability that any email is spam
    private double hamPrior;  // baseline probability that any email is ham

    /*
     * Trains the classifier by computing prior probabilities and
     * average feature values for both spam and ham emails.
     *
     * Prior probabilities reflect the natural distribution of the dataset:
     * roughly 16.7% spam and 83.3% ham, giving the model a realistic
     * starting assumption before looking at any email content.
     *
     * @param spamFeatures - EmailFeatures from all spam training emails
     * @param hamFeatures  - EmailFeatures from all ham training emails
     */
    public void train(List<EmailFeatures> spamFeatures,
                      List<EmailFeatures> hamFeatures) {

        int totalEmails = spamFeatures.size() + hamFeatures.size();

        // prior probability reflects how many of each class exist in training
        // naturally handles dataset imbalance without manual tuning
        spamPrior = (double) spamFeatures.size() / totalEmails;
        hamPrior  = (double) hamFeatures.size()  / totalEmails;

        System.out.println("Spam prior: " + String.format("%.4f", spamPrior));
        System.out.println("Ham prior:  " + String.format("%.4f", hamPrior));

        // compute average feature values for spam and ham separately
        // these become the reference points for probability scoring
        spamProbabilities = computeMeans(spamFeatures);
        hamProbabilities  = computeMeans(hamFeatures);

        System.out.println("Naive Bayes trained successfully");
    }

    /*
     * Computes the average value of each feature across a list of emails.
     * Used to build the spam and ham probability reference maps during training.
     *
     * @param featuresList - list of EmailFeatures to average
     * @return map of feature name to mean value
     */
    private Map<String, Double> computeMeans(List<EmailFeatures> featuresList) {
        Map<String, Double> sums  = new HashMap<>();
        Map<String, Double> means = new HashMap<>();
        int count = featuresList.size();

        // accumulate feature values across all emails
        for (EmailFeatures ef : featuresList) {
            for (Map.Entry<String, Double> entry : ef.getFeatures().entrySet()) {
                String key = entry.getKey();
                double val = entry.getValue();
                sums.put(key, sums.getOrDefault(key, 0.0) + val);
            }
        }

        // divide each sum by the total count to get the mean
        for (String key : sums.keySet()) {
            means.put(key, sums.get(key) / count);
        }

        return means;
    }

    /*
     * Computes a log likelihood score for an email belonging to a given class.
     * Starts with the log of the prior probability then adjusts based on
     * how the email's feature values compare to the class averages.
     *
     * A smoothing value of 0.0001 is applied to prevent log(0) crashes
     * in cases where a feature has a mean of exactly zero.
     *
     * @param email         - the email to score
     * @param probabilities - the class average feature values to compare against
     * @param prior         - the baseline probability of this class
     * @return log likelihood score for this class
     */
    private double computeScore(EmailFeatures email,
                                Map<String, Double> probabilities,
                                double prior) {
        // start with log of prior probability as the baseline
        double score = Math.log(prior);

        for (Map.Entry<String, Double> entry : email.getFeatures().entrySet()) {
            String key   = entry.getKey();
            double value = entry.getValue();

            // get the average value for this feature in this class
            double mean = probabilities.getOrDefault(key, 0.0001);

            // apply smoothing to avoid log(0) crash on zero mean features
            mean = Math.max(mean, 0.0001);

            // adjust score based on how this feature value compares to the mean
            score += value * Math.log(mean) - mean;
        }

        return score;
    }

    /*
     * Predicts whether a single email is spam or ham.
     * Computes a score for each class and returns whichever is higher.
     * No manual bias factor needed since prior probabilities handle
     * the dataset imbalance naturally.
     *
     * @param email - the EmailFeatures to classify
     * @return "spam" or "ham"
     */
    public String predict(EmailFeatures email) {
        double spamScore = computeScore(email, spamProbabilities, spamPrior);
        double hamScore  = computeScore(email, hamProbabilities,  hamPrior);
        return spamScore > hamScore ? "spam" : "ham";
    }

    /*
     * Predicts a list of emails and writes one prediction per line
     * to the specified output file.
     * Used to generate the final predictions.txt file using Naive Bayes
     * since it outperformed the centroid classifier on every metric.
     *
     * @param emails - list of EmailFeatures to predict
     * @param path   - output file path for predictions
     */
    public void predictAndWrite(List<EmailFeatures> emails, String path) {
        try (FileWriter fw = new FileWriter(path)) {
            for (EmailFeatures ef : emails) {
                String prediction = predict(ef);
                fw.write(prediction + "\n");
            }
            System.out.println("Predictions written to: " + path);
        } catch (IOException e) {
            System.out.println("Error writing predictions: " + e.getMessage());
        }
    }

    /*
     * Tests classifier accuracy against a labeled test set.
     * Reports overall accuracy as well as spam and ham accuracy separately
     * to identify whether the classifier is stronger on one class than the other.
     *
     * @param features - list of EmailFeatures for the test emails
     * @param emails   - list of Email objects with known labels to compare against
     */
    public void testAccuracy(List<EmailFeatures> features,
                             List<Email> emails) {
        int correct     = 0;
        int total       = emails.size();
        int spamCorrect = 0;
        int hamCorrect  = 0;
        int spamTotal   = 0;
        int hamTotal    = 0;

        for (int i = 0; i < total; i++) {
            String prediction = predict(features.get(i));
            String actual     = emails.get(i).getLabel() == 1 ? "spam" : "ham";

            if (actual.equals("spam")) spamTotal++;
            else hamTotal++;

            if (prediction.equals(actual)) {
                correct++;
                if (actual.equals("spam")) spamCorrect++;
                else hamCorrect++;
            }
        }

        double accuracy     = (double) correct / total * 100;
        double spamAccuracy = (double) spamCorrect / spamTotal * 100;
        double hamAccuracy  = (double) hamCorrect  / hamTotal  * 100;

        System.out.println("Total:    " + correct + "/" + total);
        System.out.println("Accuracy: " + String.format("%.2f", accuracy) + "%");
        System.out.println("\nSpam correctly identified: " + spamCorrect + "/" + spamTotal
                + " (" + String.format("%.2f", spamAccuracy) + "%)");
        System.out.println("Ham correctly identified:  " + hamCorrect  + "/" + hamTotal
                + " (" + String.format("%.2f", hamAccuracy) + "%)");
    }
}
