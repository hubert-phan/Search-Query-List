package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GEODataRetriever {

    private static final String OUTPUT_JSON_FILE = "combined_geo_results_GEOonly.json";
    private static final String EUTILS_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

    // Method to build the query URL for ESearch using a keyword
    private String buildESearchUrl(String keyword) {
        try {
            String query = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            String db = "gds"; // GEO DataSets
            String retmode = "json"; // Return format
            String retmax = "1000"; // Number of results to retrieve
    
            return EUTILS_BASE_URL + "esearch.fcgi?db=" + db + "&term=" + query + "+AND+GSE[ETYP]&retmode=" + retmode + "&retmax=" + retmax;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String executeRequest(String url) throws IOException, InterruptedException {
        System.out.println("Requesting URL: " + url);
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

        // Collect only IDs that correspond to GSE accessions
        List<String> accessions = new ArrayList<>();
        for (JsonElement idElement : idList) {
            String id = idElement.getAsString();
            // Only add GSE accessions to the list
            if (id.startsWith("200")) {
                accessions.add("GSE" + id.substring(3));
            }
        }

        return accessions;
    }

    // Method to save a single result to the JSON file with custom field names
    private void saveResult(JsonObject resultObject) {
        try (FileWriter writer = new FileWriter(OUTPUT_JSON_FILE, true);
             PrintWriter printWriter = new PrintWriter(writer)) {

            // Print the JSON with renamed fields
            printWriter.println("{");
            printWriter.printf("  \"term\": \"%s\",\n", resultObject.get("term").getAsString());
            printWriter.print("  \"GEO_IDs\": [");

            // Handle the GEO_IDs array without line breaks
            JsonArray geoIdsArray = resultObject.getAsJsonArray("GEO_IDs");
            for (int i = 0; i < geoIdsArray.size(); i++) {
                printWriter.printf("\"%s\"", geoIdsArray.get(i).getAsString());
                if (i < geoIdsArray.size() - 1) {
                    printWriter.print(", ");
                }
            }
            printWriter.println("],");

            // Print the renamed parent and root fields
            printWriter.printf("  \"parent\": \"%s\",\n", resultObject.get("parent").getAsString());
            printWriter.printf("  \"root\": \"%s\"\n", resultObject.get("root").getAsString());
            printWriter.println("},");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to search GEO data for multiple keywords using a List and export to a JSON file
    public void searchAndExportCombined(List<String> keywords, String csvFilePath) {

        for (String keyword : keywords) {
            try {
                // Retrieve GEO Accessions (GSE IDs) using the keyword
                List<String> accessions = retrieveGEODataAccessions(keyword);
                System.out.println("Retrieved Accessions for keyword '" + keyword + "': " + accessions);

                // Get the corresponding d4 and d3 values from the CSV file
                String[] d4Andd3 = getD4AndD3ForKeyword(csvFilePath, keyword);

                // Create a JSON object for this keyword and its results
                JsonObject keywordResult = new JsonObject();
                keywordResult.addProperty("term", keyword);

                JsonArray accessionArray = new JsonArray();
                for (String accession : accessions) {
                    accessionArray.add(accession);
                }
                keywordResult.add("GEO_IDs", accessionArray);
                keywordResult.addProperty("parent", d4Andd3[0]);
                keywordResult.addProperty("root", d4Andd3[1]);

                // Save this keyword's result to the JSON file
                saveResult(keywordResult);

            } catch (IOException | InterruptedException e) {
                System.err.println("Error processing keyword '" + keyword + "': " + e.getMessage());
            }
        }

        System.out.println("Successfully saved all combined results.");
    }

    // Method to get corresponding d4 and d3 values from the CSV file
    private String[] getD4AndD3ForKeyword(String csvFilePath, String keyword) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            br.readLine(); // Skip the header
    
            while ((line = br.readLine()) != null) {
                // Use the updated CSV parser that handles quoted fields
                String[] values = parseCSVLine(line);
    
                if (values[0].trim().equalsIgnoreCase(keyword.trim())) {
                    return new String[]{values[1].trim(), values[2].trim()};
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[]{"", ""}; // Return empty strings if not found
    }
    
    // Helper method to correctly parse a CSV line, respecting quoted fields with commas
    private String[] parseCSVLine(String line) {
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

    // Example usage
    public static void main(String[] args) {
        try {
            String filePath = "/Users/hubertphan/Downloads/disease_query_terms.csv"; // Replace with your file path

            // Extract keywords using KeywordExtractor
            List<String> keywords = KeywordExtractor.extractKeywordsFromFile(filePath);

            // Initialize GEODataRetriever
            GEODataRetriever retriever = new GEODataRetriever();

            // Search and export combined results for all keywords
            retriever.searchAndExportCombined(keywords, filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
