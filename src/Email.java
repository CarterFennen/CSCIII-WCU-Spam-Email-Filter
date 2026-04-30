/**
 * Email.java
 *
 * Represents a single email from the dataset.
 * Acts as the foundational data object for the entire pipeline.
 * Every other class either creates, reads, or processes Email objects
 * in some way. Contains only the raw data needed to identify and
 * classify each email.
 *
 * Design Decision: Label is stored as an integer rather than a boolean
 * to support three states: 1 for spam, 0 for ham, and -1 for emails
 * where the label is unknown, allowing the same class to handle both
 * training and prediction scenarios.
 *
 * @author Carter Fennen
 * @date April 2026
 */
public class Email {

    private int id;         // unique identifier assigned during CSV parsing
    private String rawText; // the full raw text content of the email
    private int label;      // 1 = spam, 0 = ham, -1 = unknown

    /*
     * Creates an Email object with its id, raw text, and label.
     * Called by CsvParser once per row in the dataset.
     *
     * @param id      - unique identifier assigned sequentially from 0
     * @param rawText - the full text content of the email
     * @param label   - 1 for spam, 0 for ham, -1 for unknown
     */
    public Email(int id, String rawText, int label) {
        this.id = id;
        this.rawText = rawText;
        this.label = label;
    }

    /*
     * Returns the unique id assigned to this email during parsing.
     */
    public int getID() {
        return this.id;
    }

    /*
     * Returns the full raw text of the email.
     * Used by TextProcessor to extract numerical features.
     */
    public String getRawText() {
        return this.rawText;
    }

    /*
     * Returns the label for this email.
     * Used during training to separate spam and ham feature lists.
     */
    public int getLabel() {
        return this.label;
    }

    /*
     * Placeholder for feature computation directly on the Email object.
     * Feature extraction is handled by TextProcessor in this implementation.
     */
    public EmailFeatures computeFeatures() {
        return null;
    }
}
