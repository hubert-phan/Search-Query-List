package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KeywordExtractor {

    // Method to read keywords from the file and store them in a List
    public static List<String> extractKeywordsFromFile(String filePath) {
        List<String> keywords = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip the header
            br.readLine();

            while ((line = br.readLine()) != null) {
                // Use a CSV parser that handles quoted fields
                String[] values = parseCSVLine(line);
                String d5 = values[0].trim();  // Extracting the value in the d5 column
                keywords.add(d5);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keywords;
    }

    // Helper method to correctly parse a CSV line, respecting quoted fields with commas
    private static String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                inQuotes = !inQuotes; // Toggle the state of quotes
            } else if (ch == ',' && !inQuotes) {
                // If the character is a comma and we're not inside quotes, split here
                values.add(currentField.toString().trim());
                currentField.setLength(0); // Clear the current field
            } else {
                currentField.append(ch); // Add the current character to the field
            }
        }

        // Add the last field
        values.add(currentField.toString().trim());

        return values.toArray(new String[0]);
    }

    public static void main(String[] args) {
        String filePath = "/Users/hubertphan/Downloads/smaller_query.csv";  // Replace with your file path
        List<String> keywords = extractKeywordsFromFile(filePath);

        // Display the keywords
        for (String keyword : keywords) {
            System.out.println(keyword);
        }
    }
}
