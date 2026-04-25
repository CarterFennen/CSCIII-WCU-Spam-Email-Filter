import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Carter Fennen
 * @date April 2026
 */

/*
 * CSVParser - responsible for reading the dataset from a CSV file
 * and converting each row into an Email object.
 *
 * The dataset has 3 columns: ID, raw email text and a label (1 = spam, 0 = ham).
 * Since emails contain no commas, lastIndexOf is used to reliably
 * locate the separator between ID, text and the label.
 *
 * Design Decision: BufferedReader is used instead of a Scanner because
 * it reads the file in chunks rather than character by character, making
 * it significantly more efficient for large files like this 3000 email dataset.
 */
public class CSVParser {

    /*
     * Reads the CSV file line by line and builds a list of Email objects.
     * Skips the header row and assigns each email a unique id starting at 0.
     *
     * @param path - file path to the CSV dataset
     * @return list of Email objects representing the full dataset
     */
    public List<Email> readFile(String path) {
        List<Email> emails = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader("spam_or_not_spam.csv"))) {
            String line;
            br.readLine(); // skip header row "ID1, email,label"
            int id = 0;

            while ((line = br.readLine()) != null) {

                // find the last comma to separate email text from label
                // safe to use lastIndexOf since emails contain no commas
                int lastComma = line.lastIndexOf(",");

                String text = line.substring(0, lastComma).trim();

                // strip any hidden characters before parsing the label
                // to prevent parseInt from crashing on malformed rows
                int label = Integer.parseInt(
                        line.substring(lastComma + 1)
                                .trim()
                                .replaceAll("[^0-9]", ""));

                emails.add(new Email(id++, text, label));
            }

        } catch (IOException e) {
            System.out.println("File not found: " + e.getMessage());
        }

        return emails;
    }
}
