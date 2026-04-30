import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TextProcessor.java
 *
 * Converts raw email text into numerical features that the classifiers
 * can mathematically work with. Analyzes the training set to automatically
 * discover the most differentiating words between spam and ham.
 *
 * Contains two methods:
 *   extractFeatures  - converts a single email into an EmailFeatures object
 *   findTopWords     - analyzes the training set to find the most
 *                      differentiating words between spam and ham
 *
 * Design Decision: Words are split on spaces rather than using a tokenizer
 * library because the dataset is already preprocessed. URLs are replaced
 * with "hyperlink", numbers with "NUMBER", and all text is lowercased,
 * so simple space splitting is reliable and sufficient.
 *
 * @author Carter Fennen
 * @date April 2026
 */
public class TextProcessor {

    /*
     * Converts a single email's raw text into an EmailFeatures object
     * containing 11 numerical features based on word counts.
     *
     * First builds a frequency map of every word in the email, then
     * looks up each of the 11 chosen features by name. Features were
     * selected based on their spam/ham ratio from findTopWords, meaning
     * they appear significantly more often in spam than ham.
     *
     * @param email - the Email object to extract features from
     * @return EmailFeatures object containing the 11 feature values
     */
    public EmailFeatures extractFeatures(Email email) {
        String[] words = email.getRawText().split(" ");

        // build a frequency map counting how often each word appears
        Map<String, Integer> freq = new HashMap<>();
        for (String word : words) {
            if (word.isEmpty()) continue;
            freq.put(word, freq.getOrDefault(word, 0) + 1);
        }

        EmailFeatures features = new EmailFeatures(email.getID());

        // each feature looks up its word in the frequency map
        // returns 0.0 if the word never appeared in this email
        // cast to double to match the EmailFeatures map type
        features.set("hyperlinkCount", (double) freq.getOrDefault("hyperlink",  0)); // 195x ratio - marketing spam
        features.set("kingdomCount",   (double) freq.getOrDefault("kingdom",    0)); // 97x  ratio - Nigerian fraud spam
        features.set("guaranteedCount",(double) freq.getOrDefault("guaranteed", 0)); // 69x  ratio - financial spam
        features.set("mortgageCount",  (double) freq.getOrDefault("mortgage",   0)); // 41x  ratio - financial spam
        features.set("clickCount",     (double) freq.getOrDefault("click",      0)); // 39x  ratio - marketing spam
        features.set("mailingsCount",  (double) freq.getOrDefault("mailings",   0)); // 33x  ratio - marketing spam
        features.set("moneyCount",     (double) freq.getOrDefault("money",      0)); // 15x  ratio - financial spam
        features.set("offerCount",     (double) freq.getOrDefault("offer",      0)); // 14x  ratio - marketing spam
        features.set("orderCount",     (double) freq.getOrDefault("order",      0)); // 13x  ratio - marketing spam
        features.set("freeCount",      (double) freq.getOrDefault("free",       0)); // 8x   ratio - marketing spam
        features.set("urlCount",       (double) freq.getOrDefault("URL",        0)); // ham indicator - appears more in ham

        return features;
    }

    /*
     * Analyzes the training set to find the top N words that best
     * differentiate spam from ham based on their spam/ham ratio.
     *
     * Only uses training data to avoid data leakage - looking at test
     * data to make feature decisions would artificially inflate accuracy.
     *
     * Three filters are applied to remove noise before ranking:
     *   1. Word must appear in at least 20 spam emails (removes rare words
     *      that only appear in one specific spam email)
     *   2. Word must appear in at least 1 ham email (removes words with
     *      artificially inflated ratios due to zero ham count)
     *   3. Word must be at least 3 characters (removes meaningless short words)
     *
     * Results are sorted by ratio descending and printed as a table.
     *
     * @param trainEmails - list of training Email objects to analyze
     * @param topN        - number of top words to display
     */
    public void findTopWords(List<Email> trainEmails, int topN) {

        Map<String, Integer> spamWordCounts = new HashMap<>();
        Map<String, Integer> hamWordCounts  = new HashMap<>();

        // count every word across all training emails
        // separated into spam and ham counts for ratio calculation
        for (Email email : trainEmails) {
            String[] words = email.getRawText().split(" ");

            for (String word : words) {
                if (word.isEmpty()) continue;

                if (email.getLabel() == 1) {
                    spamWordCounts.put(word,
                            spamWordCounts.getOrDefault(word, 0) + 1);
                } else {
                    hamWordCounts.put(word,
                            hamWordCounts.getOrDefault(word, 0) + 1);
                }
            }
        }

        // calculate spam/ham ratio for every word that passes the filters
        Map<String, Double> ratios = new HashMap<>();
        for (String word : spamWordCounts.keySet()) {
            double spamCount = spamWordCounts.get(word);
            double hamCount  = hamWordCounts.getOrDefault(word, 0);

            if (spamCount < 20) continue;    // filter rare words
            if (hamCount  < 1)  continue;    // filter words never seen in ham
            if (word.length() < 3) continue; // filter short meaningless words

            // higher ratio = word appears proportionally more in spam than ham
            double ratio = spamCount / hamCount;
            ratios.put(word, ratio);
        }

        // sort by ratio descending and print the top N results as a table
        System.out.printf("%-20s %-10s %-10s %-10s%n",
                "Word", "Spam", "Ham", "Ratio");
        System.out.println("-".repeat(50));

        ratios.entrySet()
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .forEach(e -> System.out.printf(
                        "%-20s %-10d %-10d %-10.2f%n",
                        e.getKey(),
                        spamWordCounts.getOrDefault(e.getKey(), 0),
                        hamWordCounts.getOrDefault(e.getKey(), 0),
                        e.getValue()
                ));
    }
}
