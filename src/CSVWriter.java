/**
 * @author Carter Fennen
 * @date April 2026
 */

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/*
 * CsvWriter - responsible for saving computed features and summary
 * statistics to CSV files for analysis and record keeping.
 *
 * Produces three output files:
 *   email_features.csv - numerical features for every email
 *   spam_summary.csv   - mean, min, max of each feature across spam emails
 *   ham_summary.csv    - mean, min, max of each feature across ham emails
 *
 * Design Decision: FileWriter is used with try-with-resources so the file
 * is automatically closed after writing even if an exception occurs,
 * preventing file corruption or resource leaks.
 */
public class CSVWriter {

    /*
     * Writes the numerical features of every email to a CSV file.
     * The first row is a header containing each feature name.
     * Each subsequent row represents one email and its feature values.
     *
     * @param features - list of EmailFeatures to write
     * @param path     - output file path
     */
    public void writeFeatures(List<EmailFeatures> features, String path) {
        try (FileWriter fw = new FileWriter(path)) {

            // write header row using feature names from the first email
            fw.write("emailId");
            Map<String, Double> firstFeatures = features.get(0).getFeatures();
            for (String key : firstFeatures.keySet()) {
                fw.write("," + key);
            }
            fw.write("\n");

            // write one row per email with its id and feature values
            for (EmailFeatures ef : features) {
                fw.write(String.valueOf(ef.getEmailId()));
                for (double value : ef.getFeatures().values()) {
                    fw.write("," + value);
                }
                fw.write("\n");
            }

            System.out.println("Features written to: " + path);

        } catch (IOException e) {
            System.out.println("Error writing features: " + e.getMessage());
        }
    }

    /*
     * Writes the summary statistics of a group of emails to a CSV file.
     * Each row contains the mean, min, and max for one feature.
     * Run separately for spam and ham to produce two model summaries.
     *
     * @param summary - FeatureSummary containing computed statistics
     * @param path    - output file path
     */
    public void writeSummary(FeatureSummary summary, String path) {
        try (FileWriter fw = new FileWriter(path)) {

            // write header row
            fw.write("feature,mean,min,max\n");

            // write one row per feature with its computed statistics
            for (String key : summary.getMeans().keySet()) {
                fw.write(key + ","
                        + summary.getMean(key) + ","
                        + summary.getMin(key)  + ","
                        + summary.getMax(key)  + "\n");
            }

            System.out.println("Summary written to: " + path);

        } catch (IOException e) {
            System.out.println("Error writing summary: " + e.getMessage());
        }
    }
}
