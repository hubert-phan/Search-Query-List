package com.example;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class KeywordExtractor {

    // Synonyms map to match different terms to the same concept
    private static final Map<String, String> SYNONYM_MAP = new HashMap<>();
    static {
        SYNONYM_MAP.put("heart attack", "myocardial infarction");
        SYNONYM_MAP.put("cancer", "neoplasm");
        // Add more synonyms as needed
    }

    // A list of stop words that should be ignored
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "the", "is", "at", "which", "on", "and", "a", "an"
    ));

    // Example MeSH terms (in a real application, this would be a comprehensive list)
    private static final Set<String> MESH_TERMS = new HashSet<>(Arrays.asList(
        "myocardial infarction", "neoplasm", "gene expression", "diabetes", "inflammation"
    ));

    /**
     * Extracts keywords from a sentence using a process similar to NCBI's keyword finder.
     * 
     * @param sentence The input sentence or query.
     * @return A list of extracted keywords.
     */
    public List<String> extractKeywords(String sentence) {
        // Tokenization: Split the sentence into words
        String[] tokens = tokenize(sentence);

        // Part-of-Speech Tagging: (Not implemented, simplified for this example)

        // Phrase Recognition & Synonym Matching
        List<String> recognizedPhrases = recognizePhrases(tokens);

        // Map recognized phrases to MeSH terms and filter out stop words
        List<String> keywords = mapToMeshTerms(recognizedPhrases);

        // Contextual Analysis: (Not implemented in this simplified version)

        return keywords;
    }

    // Tokenize the input sentence into words (simplified)
    private String[] tokenize(String sentence) {
        return sentence.toLowerCase().split("\\s+");
    }

    // Recognize phrases and match synonyms
    private List<String> recognizePhrases(String[] tokens) {
        List<String> phrases = new ArrayList<>();
        for (String token : tokens) {
            if (SYNONYM_MAP.containsKey(token)) {
                phrases.add(SYNONYM_MAP.get(token));
            } else if (!STOP_WORDS.contains(token)) {
                phrases.add(token);
            }
        }
        return phrases;
    }

    // Map recognized phrases to MeSH terms
    private List<String> mapToMeshTerms(List<String> phrases) {
        return phrases.stream()
            .filter(MESH_TERMS::contains)
            .collect(Collectors.toList());
    }

    // Example usage
    public static void main(String[] args) {
        KeywordExtractor extractor = new KeywordExtractor();
        String sentence = "A study on heart attack and cancer treatment.";
        List<String> keywords = extractor.extractKeywords(sentence);

        System.out.println("Extracted Keywords: " + keywords);
    }
}
