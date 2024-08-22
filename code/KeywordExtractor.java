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
                String[] values = line.split(",");
                String d5 = values[0].trim();  // Extracting the value in the d5 column
                keywords.add(d5);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keywords;
    }
    
    public static void main(String[] args) {
        String filePath = "/Users/hubertphan/Downloads/disease_query_terms.csv";  // Replace with your file path
        List<String> keywords = extractKeywordsFromFile(filePath);

        // Display the keywords
        for (String keyword : keywords) {
            System.out.println(keyword);
        }
    }
}
