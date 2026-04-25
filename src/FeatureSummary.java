/**
 * @author Carter Fennen
 * @date April 2026
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * FeatureSummary - computes and stores summary statistics for a group
 * of EmailFeatures objects representing either spam or ham emails.
 *
 * Calculates mean, min, and max for each feature across all emails
 * in the group. Run separately on spam and ham training emails to
 * produce two models that the classifiers use for prediction.
 *
 * Design Decision: Three separate HashMaps are used for means, mins,
 * and maxs so each statistic can be retrieved independently by name,
 * making it clean to pass individual values to the classifiers without
 * having to unpack a combined data structure.
 */
public class FeatureSummary {

    private Map<String, Double> means = new HashMap<>();
    private Map<String, Double> mins  = new HashMap<>();
    private Map<String, Double> maxs  = new HashMap<>();
    private int count;

    /*
     * Computes mean, min, and max for each feature across all emails
     * in the provided list. Initializes tracking values from the first
     * email then accumulates across the rest before computing final means.
     *
     * @param featuresList - list of EmailFeatures to summarize
     */
    public void summarize(List<EmailFeatures> featuresList) {
        count = featuresList.size();
        Map<String, Double> sums = new HashMap<>();

        // initialize sums, mins, and maxs using the first email as a baseline
        // Double.MAX_VALUE and MIN_VALUE ensure the first real value always wins
        for (Map.Entry<String, Double> entry : featuresList.get(0).getFeatures().entrySet()) {
            sums.put(entry.getKey(), 0.0);
            mins.put(entry.getKey(), Double.MAX_VALUE);
            maxs.put(entry.getKey(), Double.MIN_VALUE);
        }

        // loop through every email and accumulate feature values
        for (EmailFeatures ef : featuresList) {
            for (Map.Entry<String, Double> entry : ef.getFeatures().entrySet()) {
                String key = entry.getKey();
                double val = entry.getValue();

                // add to running sum for mean calculation later
                sums.put(key, sums.getOrDefault(key, 0.0) + val);

                // update min and max if this value is lower or higher
                if (val < mins.getOrDefault(key, Double.MAX_VALUE))
                    mins.put(key, val);
                if (val > maxs.getOrDefault(key, Double.MIN_VALUE))
                    maxs.put(key, val);
            }
        }

        // divide each sum by the total email count to get the mean
        for (String key : sums.keySet()) {
            means.put(key, sums.get(key) / count);
        }
    }

    /*
     * Returns the mean value for a given feature.
     * Used by both classifiers as the center point of each model.
     */
    public double getMean(String feature) {
        return means.getOrDefault(feature, 0.0);
    }

    /*
     * Returns the minimum observed value for a given feature
     * across all emails in this summary group.
     */
    public double getMin(String feature) {
        return mins.getOrDefault(feature, 0.0);
    }

    /*
     * Returns the maximum observed value for a given feature
     * across all emails in this summary group.
     */
    public double getMax(String feature) {
        return maxs.getOrDefault(feature, 0.0);
    }

    public int getCount() {
        return count;
    }

    public Map<String, Double> getMeans() {
        return means;
    }

    /*
     * Returns a formatted string of all feature statistics.
     * Used for printing the spam and ham summaries to the console.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Count: ").append(count).append("\n");
        for (String key : means.keySet()) {
            sb.append(key)
                    .append(" mean: ").append(String.format("%.4f", means.get(key)))
                    .append(", min: ").append(String.format("%.4f", mins.get(key)))
                    .append(", max: ").append(String.format("%.4f", maxs.get(key)))
                    .append("\n");
        }
        return sb.toString();
    }
}
