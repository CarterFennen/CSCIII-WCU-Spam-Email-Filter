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
 * SpamClassifier - classifies emails using Weighted Euclidean Distance.
 *
 * Trains on spam and ham EmailFeatures to build two FeatureSummary models
 * representing the average spam and ham email. Predicts by measuring the
 * weighted distance from a new email to each model and returning the closer one.
 *
 * Design Decision: Features are weighted based on their spam/ham ratio from
 * feature selection. Higher ratio features like hyperlink (195x) receive higher
 * weights so they have more influence on the distance calculation than weaker
 * features like free (8x). This mirrors the confidence we have in each feature
 * as a spam signal.
 *
 * Design Decision: A bias factor of 1.8 is applied to compensate for the
 * dataset imbalance of 5x more ham than spam. Without it the classifier
 * predicts almost everything as ham. The value 1.8 was found through manual
 * experimentation testing 1.5, 1.8, 2.0, and 2.5.
 */
public class SpamClassifier {

    private FeatureSummary spamModel;
    private FeatureSummary hamModel;

    /*
     * Feature weights based on spam/ham ratios from feature selection.
     * Higher ratio = higher weight = more influence on distance calculation.
     * urlCount is included as a ham indicator with a low weight.
     */
    private Map<String, Double> weights = new HashMap<>() {{
        put("hyperlinkCount",  20.0);  // 195x ratio
        put("kingdomCount",    15.0);  // 97x  ratio
        put("guaranteedCount", 12.0);  // 69x  ratio
        put("mortgageCount",   10.0);  // 41x  ratio
        put("clickCount",      10.0);  // 39x  ratio
        put("mailingsCount",    8.0);  // 33x  ratio
        put("moneyCount",       5.0);  // 15x  ratio
        put("offerCount",       5.0);  // 14x  ratio
        put("orderCount",       4.0);  // 13x  ratio
        put("freeCount",        3.0);  // 8x   ratio
        put("urlCount",         2.0);  // ham indicator
    }};

    /*
     * Trains the classifier by building a FeatureSummary model for
     * both spam and ham emails from the training set.
     * These models represent the average spam and ham email respectively
     * and serve as the reference points for all distance calculations.
     *
     * @param spamFeatures - EmailFeatures from all spam training emails
     * @param hamFeatures  - EmailFeatures from all ham training emails
     */
    public void train(List<EmailFeatures> spamFeatures,
                      List<EmailFeatures> hamFeatures) {
        spamModel = new FeatureSummary();
        hamModel  = new FeatureSummary();
        spamModel.summarize(spamFeatures);
        hamModel.summarize(hamFeatures);
        System.out.println("Models trained successfully");
    }

    /*
     * Computes the weighted Euclidean distance between an email and a model.
     * For each feature: squares the difference between the email value and
     * the model mean, multiplies by the feature weight, and adds to a running
     * sum. Returns the square root of that total.
     *
     * Squaring ensures negative differences do not cancel positive ones.
     * Weights ensure stronger spam signals have more influence on the result.
     *
     * @param email - the EmailFeatures to measure
     * @param model - the FeatureSummary model to measure distance from
     * @return weighted Euclidean distance as a double
     */
    public double distanceToModel(EmailFeatures email, FeatureSummary model) {
        double sum = 0.0;
        for (Map.Entry<String, Double> entry : email.getFeatures().entrySet()) {
            String key    = entry.getKey();
            double value  = entry.getValue();
            double mean   = model.getMean(key);
            double weight = weights.getOrDefault(key, 1.0);

            // weighted squared difference for this feature
            sum += weight * Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum);
    }

    /*
     * Predicts whether a single email is spam or ham.
     * Applies a bias factor of 1.8 to compensate for the dataset imbalance.
     * An email is predicted spam if its distance to the spam model is within
     * 1.8 times its distance to the ham model.
     *
     * @param email - the EmailFeatures to classify
     * @return "spam" or "ham"
     */
    public String predict(EmailFeatures email) {
        double distanceToSpam = distanceToModel(email, spamModel);
        double distanceToHam  = distanceToModel(email, hamModel);

        // bias factor of 1.8 makes the classifier more aggressive about
        // predicting spam to compensate for 5x more ham in the training set
        return distanceToSpam < distanceToHam * 1.8 ? "spam" : "ham";
    }

    /*
     * Predicts a list of emails and writes one prediction per line
     * to the specified output file.
     * Used to generate the final predictions.txt file.
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
     * Tracking spam and ham accuracy separately matters because overall accuracy
     * alone is misleading with imbalanced datasets. A classifier that predicts
     * everything as ham would achieve 83% overall accuracy while catching zero
     * spam emails. Separate tracking reveals whether the classifier is actually
     * learning to distinguish between the two classes.
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
