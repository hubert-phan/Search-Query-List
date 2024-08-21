package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

public class GEODataRetriever {

    private static final String EUTILS_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/";

    // Method to build the query URL for ESearch using a full sentence
    private String buildESearchUrl(String sentence) {
        try {
            String query = URLEncoder.encode(sentence, StandardCharsets.UTF_8.toString());
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

    // Method to retrieve GEO Series IDs (Accessions) from ESearch using a sentence
    public List<String> retrieveGEODataAccessions(String sentence) throws IOException, InterruptedException {
        String url = buildESearchUrl(sentence);
        String jsonResponse = executeRequest(url);

        // Save the JSON response to a file for inspection
        saveJSONToFile(jsonResponse, "esearch_result.json");

        // Parse the JSON response to extract Accessions
        return parseAccessionsFromJSON(jsonResponse);
    }

    // Method to export the data to a JSON file
    public void exportDataToJSON(List<String> accessions, String filename) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(accessions, writer);
        }
        System.out.println("Successfully saved accessions to " + filename);
    }

    // Save the raw JSON response to a file
    public void saveJSONToFile(String jsonResponse, String filename) throws IOException {
        try (FileWriter file = new FileWriter(filename)) {
            file.write(jsonResponse);
            System.out.println("Successfully saved JSON data to " + filename);
        }
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

    // Example usage
    public static void main(String[] args) {
        try {
            String sentence = "Breast cancer therapy not targeting HER2"; // Example sentence

            GEODataRetriever retriever = new GEODataRetriever();

            // Retrieve GEO Accessions (GSE IDs) using the sentence
            List<String> accessions = retriever.retrieveGEODataAccessions(sentence);
            System.out.println("Retrieved Accessions: " + accessions);

            // Export to JSON
            retriever.exportDataToJSON(accessions, "geo_accessions.json");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
