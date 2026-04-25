/**
 * @author Carter Fennen
 * @date April 2026
 */

import java.util.HashMap;
import java.util.Map;

/*
 * EmailFeatures - stores the numerical features extracted from a single email.
 *
 * Acts as a container that maps feature names to their computed values.
 * For example "hyperlinkCount" maps to 3.0 if the word "hyperlink"
 * appeared three times in that email.
 *
 * Design Decision: A HashMap is used instead of fixed fields so features
 * can be added or removed in TextProcessor without changing this class
 * at all. This keeps the feature set flexible throughout development.
 */
public class EmailFeatures {

    private int emailId;
    private Map<String, Double> features = new HashMap<>();

    /*
     * Creates an EmailFeatures container for a specific email.
     *
     * @param emailId - the id of the email these features belong to
     */
    public EmailFeatures(int emailId) {
        this.emailId = emailId;
    }

    /*
     * Stores a feature value by name.
     * Called by TextProcessor once per feature during extraction.
     *
     * @param name  - the feature name e.g. "hyperlinkCount"
     * @param value - the computed value e.g. 3.0
     */
    public void set(String name, double value) {
        features.put(name, value);
    }

    /*
     * Retrieves a feature value by name.
     * Returns 0.0 if the feature was never set.
     *
     * @param name - the feature name to look up
     * @return the feature value or 0.0 if not found
     */
    public double get(String name) {
        return features.getOrDefault(name, 0.0);
    }

    public int getEmailId() {
        return emailId;
    }

    /*
     * Returns the full feature map.
     * Used by FeatureSummary and both classifiers to access all features.
     */
    public Map<String, Double> getFeatures() {
        return features;
    }

    /*
     * Returns a readable string of the email id and all its feature values.
     * Used for quick debugging and verification during development.
     */
    @Override
    public String toString() {
        return "Email " + emailId + ": " + features.toString();
    }
}
