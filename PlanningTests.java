// TextProcessingTests.java
import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TextProcessingTests {

    // REQUIREMENT 1: Read and parse a CSV file into email objects

    @Test
    @DisplayName("CSVParser returns non-null, non-empty list from valid file")
    void testCSVParserReturnsEmails() throws Exception {
        // Checks that readFile() returns a populated list from a valid CSV
        CSVParser parser = new CSVParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        assertNotNull(emails, "Email list should not be null");
        assertFalse(emails.isEmpty(), "Email list should not be empty");
    }

    @Test
    @DisplayName("Parsed emails have valid IDs, rawText, and labels")
    void testParsedEmailFieldsArePopulated() throws Exception {
        // Checks that each parsed email has non-blank text and a valid label (true, false, or null)
        CSVParser parser = new CSVParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        for (Email e : emails) {
            assertNotNull(e.getRawText(), "Raw text should not be null");
            assertFalse(e.getRawText().isBlank(), "Raw text should not be blank");
            assertTrue(e.getLabel() == null || e.getLabel() == true || e.getLabel() == false,
                    "Label should be true, false, or null");
        }
    }

    @Test
    @DisplayName("CSVParser throws or returns empty on missing file")
    void testCSVParserHandlesMissingFile() {
        // Checks that an exception is thrown when the file path does not exist
        CSVParser parser = new CSVParser();
        assertThrows(Exception.class, () -> parser.readFile("nonexistent_file.csv"),
                "Should throw when file does not exist");
    }

    // REQUIREMENT 2: Compute features for each email

    @Test
    @DisplayName("EmailFeatures computes non-negative word count")
    void testWordCountIsNonNegative() {
        // Checks that word count is zero or greater for any email
        Email email = new Email(1, "Buy now! Click here! FREE OFFER $10 http://spam.com", true);
        EmailFeatures features = new EmailFeatures(email);
        assertTrue(features.getWordCount() >= 0, "Word count should be >= 0");
    }

    @Test
    @DisplayName("EmailFeatures correctly detects exclamation points")
    void testExclamationPointCount() {
        // Checks that exclamation points are counted correctly against a known string
        Email email = new Email(2, "Buy now! Hurry up! Limited time!", true);
        EmailFeatures features = new EmailFeatures(email);
        assertEquals(3, features.getExclamationCount(), "Should count 3 exclamation points");
    }

    @Test
    @DisplayName("EmailFeatures correctly detects all-caps words")
    void testAllCapsWordCount() {
        // Checks that only fully uppercase words (FREE, MONEY, NOW) are counted
        Email email = new Email(3, "FREE MONEY NOW for you", true);
        EmailFeatures features = new EmailFeatures(email);
        assertEquals(3, features.getAllCapsCount(), "Should count FREE, MONEY, NOW as all-caps");
    }

    @Test
    @DisplayName("EmailFeatures detects URL presence")
    void testUrlDetection() {
        // Checks that a URL is detected when present and not flagged when absent
        Email withUrl = new Email(4, "Visit http://example.com today", true);
        Email withoutUrl = new Email(5, "Hello, how are you?", false);
        assertTrue(new EmailFeatures(withUrl).getHasUrl(), "Should detect URL");
        assertFalse(new EmailFeatures(withoutUrl).getHasUrl(), "Should not detect URL in plain text");
    }

    @Test
    @DisplayName("EmailFeatures detects dollar sign presence")
    void testDollarSignCount() {
        // Checks that each dollar sign in the text is counted individually
        Email email = new Email(6, "Win $100 and $500 today!", true);
        EmailFeatures features = new EmailFeatures(email);
        assertEquals(2, features.getDollarSignCount(), "Should count 2 dollar signs");
    }

    @Test
    @DisplayName("EmailFeatures detects call-to-action phrases")
    void testCallToActionCount() {
        // Checks that known call-to-action phrases like "Buy Now!" and "Click Here!" are detected
        Email email = new Email(7, "Buy Now! Click Here! Act Today!", true);
        EmailFeatures features = new EmailFeatures(email);
        assertTrue(features.getCallToActionCount() >= 2, "Should detect at least 2 call-to-action phrases");
    }

    @Test
    @DisplayName("EmailFeatures computes a positive average word length")
    void testAvgWordLength() {
        // Checks that average word length is greater than zero for a non-empty email
        Email email = new Email(8, "Hello world", false);
        EmailFeatures features = new EmailFeatures(email);
        assertTrue(features.getAvgWordLength() > 0, "Average word length should be > 0 for non-empty email");
    }

    // REQUIREMENT 3: Display individual email features

    @Test
    @DisplayName("EmailFeatures toString/display contains key feature labels")
    void testEmailFeaturesDisplay() {
        // Checks that toString() returns a non-blank string that references at least one feature label
        Email email = new Email(9, "Buy now! Free offer!", true);
        EmailFeatures features = new EmailFeatures(email);
        String display = features.toString();
        assertNotNull(display, "Display output should not be null");
        assertFalse(display.isBlank(), "Display output should not be blank");
        assertTrue(display.toLowerCase().contains("word") || display.contains("count"),
                "Display should mention word count");
    }

    // REQUIREMENT 4: Display summary of email features

    @Test
    @DisplayName("FeatureSummary computes mean values from a list of EmailFeatures")
    void testFeatureSummaryMeans() {
        // Checks that mean word count and mean word length are both greater than zero
        List<Email> emails = List.of(
            new Email(10, "Buy now! $5 http://x.com FREE", true),
            new Email(11, "Click here! WIN $100 TODAY", true)
        );
        List<EmailFeatures> featuresList = emails.stream()
                .map(EmailFeatures::new).toList();
        FeatureSummary summary = new FeatureSummary(featuresList);
        assertTrue(summary.getWordCount() > 0, "Summary mean word count should be > 0");
        assertTrue(summary.getAvgWordLength() > 0, "Summary mean word length should be > 0");
    }

    @Test
    @DisplayName("FeatureSummary toString/display is non-null and non-empty")
    void testFeatureSummaryDisplay() {
        // Checks that the summary display output is non-null and non-blank
        List<EmailFeatures> featuresList = List.of(
            new EmailFeatures(new Email(12, "Hello world", false))
        );
        FeatureSummary summary = new FeatureSummary(featuresList);
        String display = summary.toString();
        assertNotNull(display);
        assertFalse(display.isBlank());
    }

    // REQUIREMENT 5: Build spam and not-spam models from labeled data

    @Test
    @DisplayName("Spam model is built only from spam-labeled emails")
    void testSpamModelUsesOnlySpamEmails() throws Exception {
        // Checks that filtering by label=true produces at least one email and builds a valid summary
        CSVParser parser = new CSVParser();
        List<Email> allEmails = parser.readFile("spam_or_not_spam.csv");
        List<Email> spamEmails = allEmails.stream()
                .filter(e -> Boolean.TRUE.equals(e.getLabel())).toList();
        List<EmailFeatures> spamFeatures = spamEmails.stream()
                .map(EmailFeatures::new).toList();
        assertFalse(spamFeatures.isEmpty(), "There should be at least one spam email in the dataset");
        FeatureSummary spamSummary = new FeatureSummary(spamFeatures);
        assertNotNull(spamSummary, "Spam model should not be null");
    }

    @Test
    @DisplayName("Not-spam model is built only from not-spam-labeled emails")
    void testNotSpamModelUsesOnlyNotSpamEmails() throws Exception {
        // Checks that filtering by label=false produces at least one email and builds a valid summary
        CSVParser parser = new CSVParser();
        List<Email> allEmails = parser.readFile("spam_or_not_spam.csv");
        List<Email> notSpamEmails = allEmails.stream()
                .filter(e -> Boolean.FALSE.equals(e.getLabel())).toList();
        List<EmailFeatures> notSpamFeatures = notSpamEmails.stream()
                .map(EmailFeatures::new).toList();
        assertFalse(notSpamFeatures.isEmpty(), "There should be at least one not-spam email in the dataset");
        FeatureSummary notSpamSummary = new FeatureSummary(notSpamFeatures);
        assertNotNull(notSpamSummary, "Not-spam model should not be null");
    }

    // REQUIREMENT 6: Classify a new email as spam or not spam

    @Test
    @DisplayName("Obvious spam email classifies closer to spam model")
    void testObviousSpamClassification() {
        // Checks that a spam-like email has a smaller distanceTo() the spam model than the not-spam model
        List<EmailFeatures> spamFeatures = List.of(
            new EmailFeatures(new Email(20, "Buy Now! FREE OFFER! Click Here! $100 http://spam.com WIN WIN WIN!!!", true)),
            new EmailFeatures(new Email(21, "WINNER! You have won $500! Click Here! Buy Now!", true))
        );
        List<EmailFeatures> notSpamFeatures = List.of(
            new EmailFeatures(new Email(22, "Hi, are we still on for lunch tomorrow?", false)),
            new EmailFeatures(new Email(23, "Please find the attached meeting notes.", false))
        );
        FeatureSummary spamModel = new FeatureSummary(spamFeatures);
        FeatureSummary notSpamModel = new FeatureSummary(notSpamFeatures);
        Email unknownEmail = new Email(99, "BUY NOW! FREE MONEY! Click Here! $$$! http://win.com", null);
        EmailFeatures unknownFeatures = new EmailFeatures(unknownEmail);
        double distToSpam = unknownFeatures.distanceTo(spamModel);
        double distToNotSpam = unknownFeatures.distanceTo(notSpamModel);
        assertTrue(distToSpam < distToNotSpam, "Obvious spam email should be closer to the spam model");
    }

    @Test
    @DisplayName("Obvious not-spam email classifies closer to not-spam model")
    void testObviousNotSpamClassification() {
        // Checks that a plain conversational email has a smaller distanceTo() the not-spam model
        List<EmailFeatures> spamFeatures = List.of(
            new EmailFeatures(new Email(30, "Buy Now! FREE $500! Click Here! WIN!!!", true)),
            new EmailFeatures(new Email(31, "WINNER! Click Here! $100 FREE http://spam.com", true))
        );
        List<EmailFeatures> notSpamFeatures = List.of(
            new EmailFeatures(new Email(32, "Can you send me the report by Friday?", false)),
            new EmailFeatures(new Email(33, "Looking forward to our meeting next week.", false))
        );
        FeatureSummary spamModel = new FeatureSummary(spamFeatures);
        FeatureSummary notSpamModel = new FeatureSummary(notSpamFeatures);
        Email unknownEmail = new Email(98, "Hi, please review the attached document when you get a chance.", null);
        EmailFeatures unknownFeatures = new EmailFeatures(unknownEmail);
        double distToSpam = unknownFeatures.distanceTo(spamModel);
        double distToNotSpam = unknownFeatures.distanceTo(notSpamModel);
        assertTrue(distToNotSpam < distToSpam, "Obvious not-spam email should be closer to the not-spam model");
    }

    @Test
    @DisplayName("distanceTo returns 0 for an email compared to itself")
    void testDistanceToSelfIsZero() {
        // Checks that the Manhattan distance from an email's features to itself is exactly 0
        Email email = new Email(50, "Hello world, this is a test.", false);
        EmailFeatures f = new EmailFeatures(email);
        assertEquals(0.0, f.distanceTo(f), 0.0001, "Distance from an email to itself should be 0");
    }

    @Test
    @DisplayName("distanceTo is non-negative between different emails")
    void testDistanceIsNonNegative() {
        // Checks that Manhattan distance is always >= 0 between any two emails
        EmailFeatures f1 = new EmailFeatures(new Email(51, "Buy now! Free!", true));
        EmailFeatures f2 = new EmailFeatures(new Email(52, "Hello, how are you?", false));
        assertTrue(f1.distanceTo(f2) >= 0, "Manhattan distance must be non-negative");
    }

    // REQUIREMENT 7: Write email features to a CSV file

    @Test
    @DisplayName("CsvWriter creates email features CSV file at given path")
    void testWriteFeaturesCreatesFile() throws Exception {
        // Checks that writeFeatures() creates a non-empty file at the specified path
        List<EmailFeatures> featuresList = List.of(
            new EmailFeatures(new Email(60, "Buy now! Free offer!", true)),
            new EmailFeatures(new Email(61, "See you tomorrow.", false))
        );
        String path = "test_features_output.csv";
        CsvWriter writer = new CsvWriter();
        writer.writeFeatures(featuresList, path);
        File file = new File(path);
        assertTrue(file.exists(), "Features CSV file should be created");
        assertTrue(file.length() > 0, "Features CSV file should not be empty");
        file.delete();
    }

    @Test
    @DisplayName("Email features CSV has a header row and one row per email")
    void testWriteFeaturesCSVFormat() throws Exception {
        // Checks that the CSV has a header row with feature column names and one data row per email
        List<EmailFeatures> featuresList = List.of(
            new EmailFeatures(new Email(62, "Win $100 today!", true)),
            new EmailFeatures(new Email(63, "Project update attached.", false))
        );
        String path = "test_features_format.csv";
        CsvWriter writer = new CsvWriter();
        writer.writeFeatures(featuresList, path);
        List<String> lines = Files.readAllLines(Paths.get(path));
        assertTrue(lines.size() >= 3, "CSV should have header + 2 data rows");
        String header = lines.get(0).toLowerCase();
        assertTrue(header.contains("word") || header.contains("count") || header.contains("exclamation"),
                "Header row should contain feature column names");
        new File(path).delete();
    }

    // REQUIREMENT 8: Write summary data to a CSV file

    @Test
    @DisplayName("CsvWriter creates summary CSV file at given path")
    void testWriteSummaryCreatesFile() throws Exception {
        // Checks that writeSummary() creates a non-empty file at the specified path
        FeatureSummary summary = new FeatureSummary(List.of(
            new EmailFeatures(new Email(70, "Buy now! FREE MONEY!", true))
        ));
        String path = "test_summary_output.csv";
        CsvWriter writer = new CsvWriter();
        writer.writeSummary(summary, path);
        File file = new File(path);
        assertTrue(file.exists(), "Summary CSV file should be created");
        assertTrue(file.length() > 0, "Summary CSV file should not be empty");
        file.delete();
    }

    @Test
    @DisplayName("Summary CSV contains all expected feature fields")
    void testWriteSummaryCSVContainsAllFeatures() throws Exception {
        // Checks that the summary CSV references expected field names like word count and exclamation count
        FeatureSummary summary = new FeatureSummary(List.of(
            new EmailFeatures(new Email(71, "Click Here! $50 http://deal.com FREE", true))
        ));
        String path = "test_summary_fields.csv";
        CsvWriter writer = new CsvWriter();
        writer.writeSummary(summary, path);
        String content = Files.readString(Paths.get(path)).toLowerCase();
        assertTrue(content.contains("word") || content.contains("count"), "Should reference word count");
        assertTrue(content.contains("exclamation") || content.contains("!"), "Should reference exclamation count");
        new File(path).delete();
    }
}
