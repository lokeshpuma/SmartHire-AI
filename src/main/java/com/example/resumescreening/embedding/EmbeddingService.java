package com.example.resumescreening.embedding;

public interface EmbeddingService {
    EmbeddingResult embedText(String text);
    double calculateSimilarity(String textA, String textB);
    String getModelName();
    String getModelVersion();
    int getDimension();
}
