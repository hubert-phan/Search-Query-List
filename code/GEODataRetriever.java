package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

public class GEODataRetriever {

    private static final String EUTILS_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

    // Method to build the query URL for ESearch using a full sentence or keyword
    private String buildESearchUrl(String keyword) {
        try {
            String query = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            String db = "gds"; // GEO DataSets
            String retmode = "json"; // Return format
            String retmax = "1000"; // Number of results to retrieve

            return EUTILS_BASE_URL + "esearch.fcgi?db=" + db + "&term=" + query + "&retmode=" + retmode + "&retmax=" + retmax;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to execute the API request
    private String executeRequest(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new IOException("Error retrieving data: " + response.statusCode());
        }
    }

    // Method to retrieve GEO Series IDs (Accessions) from ESearch using a keyword
    public List<String> retrieveGEODataAccessions(String keyword) throws IOException, InterruptedException {
        String url = buildESearchUrl(keyword);
        String jsonResponse = executeRequest(url);

        // Parse the JSON response to extract Accessions
        return parseAccessionsFromJSON(jsonResponse);
    }

    // Method to parse GEO Series Accessions from JSON response
    private List<String> parseAccessionsFromJSON(String jsonResponse) {
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

        // Navigate to the "esearchresult" object
        JsonObject esearchResult = jsonObject.getAsJsonObject("esearchresult");

        // Retrieve the "idlist" array from the "esearchresult" object
        JsonArray idList = esearchResult.getAsJsonArray("idlist");

        // Collect all IDs from the array and then convert them to accessions (GSE IDs)
        List<String> accessions = new ArrayList<>();
        for (JsonElement idElement : idList) {
            String id = idElement.getAsString();

            // Convert the ID to Accession by removing the "200" prefix after "GSE"
            accessions.add(getGEOAccession(id));
        }

        return accessions;
    }

    // Method to convert UID to GEO Accession (GSE ID)
    private String getGEOAccession(String uid) {
        // Assuming the UID format is "200123456", we convert it to "GSE123456"
        if (uid.startsWith("200")) {
            uid = uid.substring(3); // Remove the "200" prefix
        }
        return "GSE" + uid;
    }

    // Method to search GEO data for multiple keywords using a Set and export to a single JSON file
    public void searchAndExportCombined(Set<String> keywords, String csvFilePath) {
        JsonArray combinedResults = new JsonArray();

        for (String keyword : keywords) {
            try {
                // Retrieve GEO Accessions (GSE IDs) using the keyword
                List<String> accessions = retrieveGEODataAccessions(keyword);
                System.out.println("Retrieved Accessions for keyword '" + keyword + "': " + accessions);

                // Get the corresponding d4 and d3 values from the CSV file
                String[] d4Andd3 = getD4AndD3ForKeyword(csvFilePath, keyword);

                // Create a JSON object for this keyword and its results
                JsonObject keywordResult = new JsonObject();
                keywordResult.addProperty("d5", keyword);
                
                JsonArray accessionArray = new JsonArray();
                for (String accession : accessions) {
                    accessionArray.add(accession);
                }
                keywordResult.add("GEO_IDs", accessionArray);
                keywordResult.addProperty("d4", d4Andd3[0]);
                keywordResult.addProperty("d3", d4Andd3[1]);

                // Add this keyword's result to the combined results array
                combinedResults.add(keywordResult);

            } catch (IOException | InterruptedException e) {
                System.err.println("Error processing keyword '" + keyword + "': " + e.getMessage());
            }
        }

        // Export the combined results to a single JSON file
        exportCombinedResultsToJSON(combinedResults, "combined_geo_results.json");
    }

    // Method to get corresponding d4 and d3 values from the CSV file
    private String[] getD4AndD3ForKeyword(String csvFilePath, String keyword) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            br.readLine(); // Skip the header

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values[0].trim().equalsIgnoreCase(keyword.trim())) {
                    return new String[]{values[1].trim(), values[2].trim()};
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[]{"", ""}; // Return empty strings if not found
    }

    // Method to export combined results to a JSON file
    public void exportCombinedResultsToJSON(JsonArray combinedResults, String filename) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(combinedResults, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Successfully saved combined results to " + filename);
    }

    // Example usage
    public static void main(String[] args) {
        try {
            String filePath = "/Users/hubertphan/Downloads/disease_query_terms.csv"; // Replace with your file path

            // Extract keywords using KeywordExtractor
            Set<String> keywords = KeywordExtractor.extractKeywordsFromFile(filePath);

            // Initialize GEODataRetriever
            GEODataRetriever retriever = new GEODataRetriever();

            // Search and export combined results for all keywords
            retriever.searchAndExportCombined(keywords, filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
