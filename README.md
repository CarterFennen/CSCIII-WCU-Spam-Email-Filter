# CSCIII-WCU-Spam-Email-Filter

##System Architecture (UML)
```mermaid
classDiagram
    direction TB

    class Main {
        +main(String[] args) void
    }

    class CSVParser {
        +readFile(String path) List~Email~
    }

    class Email {
        -int id
        -String rawText
        -int label
        +Email(int id, String rawText, int label)
        +getID() int
        +getRawText() String
        +getLabel() int
        +computeFeatures() EmailFeatures
    }

    class TextProcessor {
        +extractFeatures(Email email) EmailFeatures
        +findTopWords(List~Email~ trainEmails, int topN) void
    }

    class EmailFeatures {
        -int emailId
        -Map~String, Double~ features
        +EmailFeatures(int emailId)
        +set(String name, double value) void
        +get(String name) double
        +getEmailId() int
        +getFeatures() Map~String, Double~
        +toString() String
    }

    class FeatureSummary {
        -Map~String, Double~ means
        -Map~String, Double~ mins
        -Map~String, Double~ maxs
        -int count
        +summarize(List~EmailFeatures~ featuresList) void
        +getMean(String feature) double
        +getMin(String feature) double
        +getMax(String feature) double
        +getMeans() Map~String, Double~
        +normalize(String feature, double value) double
        +toString() String
    }

    class CsvWriter {
        +writeFeatures(List~EmailFeatures~ features, String path) void
        +writeSummary(FeatureSummary summary, String path) void
    }

    class SpamClassifier {
        -FeatureSummary spamModel
        -FeatureSummary hamModel
        -Map~String, Double~ weights
        +train(List~EmailFeatures~ spamFeatures, List~EmailFeatures~ hamFeatures) void
        +distanceToModel(EmailFeatures email, FeatureSummary model) double
        +predict(EmailFeatures email) String
        +predictAndWrite(List~EmailFeatures~ emails, String path) void
        +testAccuracy(List~EmailFeatures~ features, List~Email~ emails) void
    }

    class NaiveBayesClassifier {
        -Map~String, Double~ spamProbabilities
        -Map~string, Double~ hamProbabilities
        -double spamPrior
        -double hamPrior
        +train(List~EmailFeatures~ spamFeatures, List~EmailFeatures~ hamFeatures) void
        +predict(EmailFeatures email) String
        +testAccuracy(List~EmailFeatures~ features, List~Email~ emails) void
        -computeMeans(List~EmailFeatures~ featuresList) Map~String, Double~
        -computeScore(EmailFeatures email, Map~String, Double~ probabilities, double prior) double
    }

    Main --> CSVParser : 1. reads CSV
    Main --> TextProcessor : 2. extracts features
    Main --> FeatureSummary : 3. builds models
    Main --> SpamClassifier : 4. trains and tests
    Main --> NaiveBayesClassifier : 5. trains and tests
    Main --> CsvWriter : 6. writes output

    CSVParser --> Email : creates
    TextProcessor --> EmailFeatures : produces
    TextProcessor ..> Email : reads
    EmailFeatures ..> Email : belongs to
    FeatureSummary ..> EmailFeatures : summarizes
    SpamClassifier --> FeatureSummary : uses
    SpamClassifier ..> EmailFeatures : classifies
    NaiveBayesClassifier ..> EmailFeatures : classifies
    CsvWriter ..> EmailFeatures : writes
    CsvWriter ..> FeatureSummary : writes
```
