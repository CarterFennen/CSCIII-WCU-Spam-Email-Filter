import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TextProcessingTests.java
 *
 * Unit tests for the spam filter pipeline.
 * Tests each requirement against the current implementation.
 *
 * @author Carter Fennen
 * @date April 2026
 */
public class TextProcessingTests {

    // REQUIREMENT 1: Read and parse a CSV file into Email objects

    @Test
    @DisplayName("CsvParser returns non-null, non-empty list from valid file")
    void testCsvParserReturnsEmails() {
        // Checks that readFile() returns a populated list from a valid CSV
        CsvParser parser = new CsvParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        assertNotNull(emails, "Email list should not be null");
        assertFalse(emails.isEmpty(), "Email list should not be empty");
    }

    @Test
    @DisplayName("CsvParser loads exactly 3000 emails from dataset")
    void testCsvParserLoads3000Emails() {
        // Checks that all 3000 rows are parsed correctly
        CsvParser parser = new CsvParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        assertEquals(3000, emails.size(), "Dataset should contain exactly 3000 emails");
    }

    @Test
    @DisplayName("Parsed emails have valid IDs, rawText, and labels")
    void testParsedEmailFieldsArePopulated() {
        // Checks that each parsed email has non-blank text and a valid label (0 or 1)
        CsvParser parser = new CsvParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        for (Email e : emails) {
            assertNotNull(e.getRawText(), "Raw text should not be null");
            assertFalse(e.getRawText().isBlank(), "Raw text should not be blank");
            assertTrue(e.getLabel() == 0 || e.getLabel() == 1,
                    "Label should be 0 (ham) or 1 (spam)");
        }
    }

    @Test
    @DisplayName("CsvParser returns empty list on missing file")
    void testCsvParserHandlesMissingFile() {
        // Checks that an empty list is returned when the file path does not exist
        CsvParser parser = new CsvParser();
        List<Email> emails = parser.readFile("nonexistent_file.csv");
        assertTrue(emails.isEmpty(), "Should return empty list when file does not exist");
    }

    @Test
    @DisplayName("Dataset contains both spam and ham emails")
    void testDatasetContainsBothClasses() {
        // Checks that the dataset has at least one spam and one ham email
        CsvParser parser = new CsvParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        long spamCount = emails.stream().filter(e -> e.getLabel() == 1).count();
        long hamCount  = emails.stream().filter(e -> e.getLabel() == 0).count();
        assertTrue(spamCount > 0, "Dataset should contain at least one spam email");
        assertTrue(hamCount  > 0, "Dataset should contain at least one ham email");
    }

    // REQUIREMENT 2: Extract numerical features from each email

    @Test
    @DisplayName("TextProcessor returns non-null EmailFeatures for any email")
    void testExtractFeaturesReturnsNonNull() {
        // Checks that extractFeatures() never returns null
        TextProcessor tp = new TextProcessor();
        Email email = new Email(1, "click here free offer hyperlink", 1);
        EmailFeatures features = tp.extractFeatures(email);
        assertNotNull(features, "EmailFeatures should not be null");
    }

    @Test
    @DisplayName("EmailFeatures emailId matches the source email id")
    void testEmailFeaturesIdMatchesEmail() {
        // Checks that the EmailFeatures object is linked to the correct email
        TextProcessor tp = new TextProcessor();
        Email email = new Email(42, "click here free offer", 1);
        EmailFeatures features = tp.extractFeatures(email);
        assertEquals(42, features.getEmailId(), "EmailFeatures id should match source email id");
    }

    @Test
    @DisplayName("EmailFeatures contains all 11 expected features")
    void testEmailFeaturesContainsAllFeatures() {
        // Checks that all 11 features are present in the feature map
        TextProcessor tp = new TextProcessor();
        Email email = new Email(1, "click here free offer hyperlink kingdom guaranteed mortgage mailings money order", 1);
        EmailFeatures features = tp.extractFeatures(email);
        List<String> expectedFeatures = List.of(
            "hyperlinkCount", "kingdomCount", "guaranteedCount",
            "mortgageCount", "clickCount", "mailingsCount",
            "moneyCount", "offerCount", "orderCount",
            "freeCount", "urlCount"
        );
        for (String feature : expectedFeatures) {
            assertTrue(features.getFeatures().containsKey(feature),
                    "Features should contain: " + feature);
        }
    }

    @Test
    @DisplayName("hyperlinkCount correctly counts hyperlink token")
    void testHyperlinkCountIsCorrect() {
        // Checks that hyperlink appearances are counted correctly
        TextProcessor tp = new TextProcessor();
        Email email = new Email(2, "hyperlink hyperlink hyperlink", 1);
        EmailFeatures features = tp.extractFeatures(email);
        assertEquals(3.0, features.get("hyperlinkCount"), 0.001,
                "hyperlinkCount should be 3");
    }

    @Test
    @DisplayName("Feature values are zero for words not present in email")
    void testAbsentFeaturesAreZero() {
        // Checks that features default to 0 when their word does not appear
        TextProcessor tp = new TextProcessor();
        Email email = new Email(3, "hello how are you today", 0);
        EmailFeatures features = tp.extractFeatures(email);
        assertEquals(0.0, features.get("hyperlinkCount"), 0.001,
                "hyperlinkCount should be 0 for plain email");
        assertEquals(0.0, features.get("freeCount"), 0.001,
                "freeCount should be 0 for plain email");
    }

    @Test
    @DisplayName("Feature values are non-negative for any email")
    void testFeatureValuesAreNonNegative() {
        // Checks that no feature value is negative
        TextProcessor tp = new TextProcessor();
        CsvParser parser = new CsvParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        for (Email e : emails.subList(0, 10)) {
            EmailFeatures features = tp.extractFeatures(e);
            for (double value : features.getFeatures().values()) {
                assertTrue(value >= 0, "No feature value should be negative");
            }
        }
    }

    // REQUIREMENT 3: Build spam and ham summary models

    @Test
    @DisplayName("FeatureSummary produces non-null model from spam features")
    void testSpamSummaryIsNonNull() {
        // Checks that a spam model can be built from spam training features
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> spamFeatures = List.of(
            tp.extractFeatures(new Email(1, "click here free hyperlink offer money", 1)),
            tp.extractFeatures(new Email(2, "hyperlink guaranteed free click money", 1))
        );
        FeatureSummary spamSummary = new FeatureSummary();
        spamSummary.summarize(spamFeatures);
        assertNotNull(spamSummary, "Spam summary should not be null");
    }

    @Test
    @DisplayName("FeatureSummary mean for hyperlinkCount is correct")
    void testSpamSummaryMeanIsCorrect() {
        // Checks that the mean hyperlinkCount is computed correctly across two emails
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> features = List.of(
            tp.extractFeatures(new Email(1, "hyperlink hyperlink", 1)),
            tp.extractFeatures(new Email(2, "hyperlink", 1))
        );
        FeatureSummary summary = new FeatureSummary();
        summary.summarize(features);
        assertEquals(1.5, summary.getMean("hyperlinkCount"), 0.001,
                "Mean hyperlinkCount should be 1.5");
    }

    @Test
    @DisplayName("FeatureSummary min and max are computed correctly")
    void testSummaryMinAndMaxAreCorrect() {
        // Checks that min and max values are tracked correctly across emails
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> features = List.of(
            tp.extractFeatures(new Email(1, "hyperlink hyperlink hyperlink", 1)),
            tp.extractFeatures(new Email(2, "hello world", 0))
        );
        FeatureSummary summary = new FeatureSummary();
        summary.summarize(features);
        assertEquals(0.0, summary.getMin("hyperlinkCount"), 0.001,
                "Min hyperlinkCount should be 0");
        assertEquals(3.0, summary.getMax("hyperlinkCount"), 0.001,
                "Max hyperlinkCount should be 3");
    }

    @Test
    @DisplayName("FeatureSummary count matches number of emails summarized")
    void testSummaryCountIsCorrect() {
        // Checks that the count field reflects the number of emails in the summary
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> features = List.of(
            tp.extractFeatures(new Email(1, "free click hyperlink", 1)),
            tp.extractFeatures(new Email(2, "money offer order", 1)),
            tp.extractFeatures(new Email(3, "guaranteed mortgage mailings", 1))
        );
        FeatureSummary summary = new FeatureSummary();
        summary.summarize(features);
        assertEquals(3, summary.getCount(), "Summary count should match number of emails");
    }

    // REQUIREMENT 4: Classify emails using Weighted Centroid

    @Test
    @DisplayName("SpamClassifier predicts spam for obvious spam email")
    void testCentroidPredictsSPamForObviousSpam() {
        // Checks that a spam-like email is classified as spam
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> spamFeatures = List.of(
            tp.extractFeatures(new Email(1, "hyperlink hyperlink free click offer money guaranteed", 1)),
            tp.extractFeatures(new Email(2, "hyperlink click free offer kingdom mortgage mailings", 1))
        );
        List<EmailFeatures> hamFeatures = List.of(
            tp.extractFeatures(new Email(3, "hello how are you doing today", 0)),
            tp.extractFeatures(new Email(4, "meeting tomorrow at the office please confirm", 0))
        );
        SpamClassifier classifier = new SpamClassifier();
        classifier.train(spamFeatures, hamFeatures);
        EmailFeatures testEmail = tp.extractFeatures(
            new Email(99, "hyperlink hyperlink hyperlink free click offer money guaranteed", 1));
        assertEquals("spam", classifier.predict(testEmail),
                "Obvious spam email should be classified as spam");
    }

    @Test
    @DisplayName("SpamClassifier predicts ham for obvious ham email")
    void testCentroidPredictsHamForObviousHam() {
        // Checks that a plain conversational email is classified as ham
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> spamFeatures = List.of(
            tp.extractFeatures(new Email(1, "hyperlink hyperlink free click offer money guaranteed", 1)),
            tp.extractFeatures(new Email(2, "hyperlink click free offer kingdom mortgage mailings", 1))
        );
        List<EmailFeatures> hamFeatures = List.of(
            tp.extractFeatures(new Email(3, "hello how are you doing today", 0)),
            tp.extractFeatures(new Email(4, "meeting tomorrow at the office please confirm", 0))
        );
        SpamClassifier classifier = new SpamClassifier();
        classifier.train(spamFeatures, hamFeatures);
        EmailFeatures testEmail = tp.extractFeatures(
            new Email(98, "please find the attached document for your review", 0));
        assertEquals("ham", classifier.predict(testEmail),
                "Obvious ham email should be classified as ham");
    }

    @Test
    @DisplayName("SpamClassifier distanceToModel returns non-negative value")
    void testDistanceToModelIsNonNegative() {
        // Checks that distance calculation always returns a non-negative value
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> spamFeatures = List.of(
            tp.extractFeatures(new Email(1, "hyperlink free click offer money", 1))
        );
        List<EmailFeatures> hamFeatures = List.of(
            tp.extractFeatures(new Email(2, "hello how are you", 0))
        );
        SpamClassifier classifier = new SpamClassifier();
        classifier.train(spamFeatures, hamFeatures);
        EmailFeatures testEmail = tp.extractFeatures(new Email(99, "free click hyperlink", 1));
        FeatureSummary spamModel = new FeatureSummary();
        spamModel.summarize(spamFeatures);
        assertTrue(classifier.distanceToModel(testEmail, spamModel) >= 0,
                "Distance to model should be non-negative");
    }

    // REQUIREMENT 5: Classify emails using Naive Bayes

    @Test
    @DisplayName("NaiveBayesClassifier predicts spam for obvious spam email")
    void testNaiveBayesPredictsSPamForObviousSpam() {
        // Checks that a spam-like email is classified as spam by Naive Bayes
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> spamFeatures = List.of(
            tp.extractFeatures(new Email(1, "hyperlink hyperlink free click offer money guaranteed", 1)),
            tp.extractFeatures(new Email(2, "hyperlink click free offer kingdom mortgage mailings", 1))
        );
        List<EmailFeatures> hamFeatures = List.of(
            tp.extractFeatures(new Email(3, "hello how are you doing today", 0)),
            tp.extractFeatures(new Email(4, "meeting tomorrow at the office please confirm", 0))
        );
        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(spamFeatures, hamFeatures);
        EmailFeatures testEmail = tp.extractFeatures(
            new Email(99, "hyperlink hyperlink hyperlink free click offer money guaranteed", 1));
        assertEquals("spam", nb.predict(testEmail),
                "Obvious spam email should be classified as spam by Naive Bayes");
    }

    @Test
    @DisplayName("NaiveBayesClassifier predicts ham for obvious ham email")
    void testNaiveBayesPredictsHamForObviousHam() {
        // Checks that a plain conversational email is classified as ham by Naive Bayes
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> spamFeatures = List.of(
            tp.extractFeatures(new Email(1, "hyperlink hyperlink free click offer money guaranteed", 1)),
            tp.extractFeatures(new Email(2, "hyperlink click free offer kingdom mortgage mailings", 1))
        );
        List<EmailFeatures> hamFeatures = List.of(
            tp.extractFeatures(new Email(3, "hello how are you doing today", 0)),
            tp.extractFeatures(new Email(4, "meeting tomorrow at the office please confirm", 0))
        );
        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(spamFeatures, hamFeatures);
        EmailFeatures testEmail = tp.extractFeatures(
            new Email(98, "please find the attached document for your review", 0));
        assertEquals("ham", nb.predict(testEmail),
                "Obvious ham email should be classified as ham by Naive Bayes");
    }

    @Test
    @DisplayName("NaiveBayesClassifier achieves at least 85% accuracy on full dataset")
    void testNaiveBayesAccuracyOnFullDataset() {
        // Checks that Naive Bayes achieves at least 85% accuracy on the full dataset
        CsvParser parser = new CsvParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        Collections.shuffle(emails, new Random(42));
        int splitIndex = (int)(emails.size() * 0.80);
        List<Email> trainEmails = emails.subList(0, splitIndex);
        List<Email> testEmails  = emails.subList(splitIndex, emails.size());

        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> spamFeatures = new ArrayList<>();
        List<EmailFeatures> hamFeatures  = new ArrayList<>();
        List<EmailFeatures> testFeatures = new ArrayList<>();

        for (Email e : trainEmails) {
            EmailFeatures ef = tp.extractFeatures(e);
            if (e.getLabel() == 1) spamFeatures.add(ef);
            else hamFeatures.add(ef);
        }
        for (Email e : testEmails) {
            testFeatures.add(tp.extractFeatures(e));
        }

        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(spamFeatures, hamFeatures);

        int correct = 0;
        for (int i = 0; i < testEmails.size(); i++) {
            String prediction = nb.predict(testFeatures.get(i));
            String actual     = testEmails.get(i).getLabel() == 1 ? "spam" : "ham";
            if (prediction.equals(actual)) correct++;
        }

        double accuracy = (double) correct / testEmails.size() * 100;
        assertTrue(accuracy >= 85.0,
                "Naive Bayes should achieve at least 85% accuracy, got: " + accuracy + "%");
    }

    // REQUIREMENT 6: Write features and summaries to CSV files

    @Test
    @DisplayName("CsvWriter creates email features CSV file at given path")
    void testWriteFeaturesCreatesFile() throws Exception {
        // Checks that writeFeatures() creates a non-empty file at the specified path
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> featuresList = List.of(
            tp.extractFeatures(new Email(1, "click here free hyperlink", 1)),
            tp.extractFeatures(new Email(2, "hello how are you", 0))
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
    @DisplayName("Email features CSV has header row and one row per email")
    void testWriteFeaturesCSVFormat() throws Exception {
        // Checks that the CSV has a header row and one data row per email
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> featuresList = List.of(
            tp.extractFeatures(new Email(1, "click free hyperlink offer money", 1)),
            tp.extractFeatures(new Email(2, "hello world meeting tomorrow", 0))
        );
        String path = "test_features_format.csv";
        CsvWriter writer = new CsvWriter();
        writer.writeFeatures(featuresList, path);
        List<String> lines = Files.readAllLines(Paths.get(path));
        assertTrue(lines.size() >= 3, "CSV should have header plus 2 data rows");
        String header = lines.get(0).toLowerCase();
        assertTrue(header.contains("emailid"), "Header should contain emailId column");
        new File(path).delete();
    }

    @Test
    @DisplayName("CsvWriter creates summary CSV file at given path")
    void testWriteSummaryCreatesFile() throws Exception {
        // Checks that writeSummary() creates a non-empty file at the specified path
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> featuresList = List.of(
            tp.extractFeatures(new Email(1, "click free hyperlink offer money guaranteed", 1))
        );
        FeatureSummary summary = new FeatureSummary();
        summary.summarize(featuresList);
        String path = "test_summary_output.csv";
        CsvWriter writer = new CsvWriter();
        writer.writeSummary(summary, path);
        File file = new File(path);
        assertTrue(file.exists(), "Summary CSV file should be created");
        assertTrue(file.length() > 0, "Summary CSV file should not be empty");
        file.delete();
    }

    @Test
    @DisplayName("Summary CSV contains feature mean min and max columns")
    void testWriteSummaryCSVContainsColumns() throws Exception {
        // Checks that the summary CSV header contains feature, mean, min, and max columns
        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> featuresList = List.of(
            tp.extractFeatures(new Email(1, "click free hyperlink offer money", 1))
        );
        FeatureSummary summary = new FeatureSummary();
        summary.summarize(featuresList);
        String path = "test_summary_columns.csv";
        CsvWriter writer = new CsvWriter();
        writer.writeSummary(summary, path);
        String content = Files.readString(Paths.get(path)).toLowerCase();
        assertTrue(content.contains("feature"), "Summary CSV should contain feature column");
        assertTrue(content.contains("mean"),    "Summary CSV should contain mean column");
        assertTrue(content.contains("min"),     "Summary CSV should contain min column");
        assertTrue(content.contains("max"),     "Summary CSV should contain max column");
        new File(path).delete();
    }

    @Test
    @DisplayName("Predictions file is created and contains one prediction per line")
    void testPredictionsFileFormat() throws Exception {
        // Checks that predictions.txt is created with one spam or ham prediction per line
        CsvParser parser = new CsvParser();
        List<Email> emails = parser.readFile("spam_or_not_spam.csv");
        Collections.shuffle(emails, new Random(42));
        int splitIndex = (int)(emails.size() * 0.80);
        List<Email> trainEmails = emails.subList(0, splitIndex);
        List<Email> testEmails  = emails.subList(splitIndex, emails.size());

        TextProcessor tp = new TextProcessor();
        List<EmailFeatures> spamFeatures = new ArrayList<>();
        List<EmailFeatures> hamFeatures  = new ArrayList<>();
        List<EmailFeatures> testFeatures = new ArrayList<>();

        for (Email e : trainEmails) {
            EmailFeatures ef = tp.extractFeatures(e);
            if (e.getLabel() == 1) spamFeatures.add(ef);
            else hamFeatures.add(ef);
        }
        for (Email e : testEmails) {
            testFeatures.add(tp.extractFeatures(e));
        }

        NaiveBayesClassifier nb = new NaiveBayesClassifier();
        nb.train(spamFeatures, hamFeatures);

        String path = "test_predictions.txt";
        nb.predictAndWrite(testFeatures, path);

        List<String> lines = Files.readAllLines(Paths.get(path));
        assertEquals(testEmails.size(), lines.size(),
                "Predictions file should have one line per test email");
        for (String line : lines) {
            assertTrue(line.equals("spam") || line.equals("ham"),
                    "Each prediction should be spam or ham, got: " + line);
        }
        new File(path).delete();
    }
}
