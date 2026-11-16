package com.example.resumescreening.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalDeterministicEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(LocalDeterministicEmbeddingService.class);

    private final String modelName;
    private final String modelVersion = "1.0.0-cpu";
    private final int dimension;

    // Cache embeddings by content SHA-256 hash
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    public LocalDeterministicEmbeddingService(
            @Value("${app.embedding.model-name:dense-subword-v1}") String modelName,
            @Value("${app.embedding.dimension:128}") int dimension) {
        this.modelName = modelName;
        this.dimension = dimension;
        log.info("Initialized LocalDeterministicEmbeddingService: model={}, dimension={}", modelName, dimension);
    }

    @Override
    public EmbeddingResult embedText(String text) {
        if (text == null || text.isBlank()) {
            float[] emptyVec = new float[dimension];
            return new EmbeddingResult(emptyVec, dimension, modelName, modelVersion);
        }

        String hash = hashContent(text);
        float[] vector = embeddingCache.computeIfAbsent(hash, h -> computeDenseVector(text));
        return new EmbeddingResult(vector, dimension, modelName, modelVersion);
    }

    @Override
    public double calculateSimilarity(String textA, String textB) {
        EmbeddingResult embA = embedText(textA);
        EmbeddingResult embB = embedText(textB);
        return VectorMath.cosineSimilarity(embA.getVector(), embB.getVector());
    }

    @Override
    public String getModelName() { return modelName; }

    @Override
    public String getModelVersion() { return modelVersion; }

    @Override
    public int getDimension() { return dimension; }

    private float[] computeDenseVector(String text) {
        float[] vector = new float[dimension];
        String clean = text.toLowerCase().replaceAll("[^a-z0-9+#.\\s]", " ");
        String[] tokens = clean.split("\\s+");

        for (String token : tokens) {
            if (token.isBlank()) continue;

            // Token feature hash
            int hash1 = Math.abs(token.hashCode()) % dimension;
            vector[hash1] += 1.0f;

            // Character trigrams
            if (token.length() >= 3) {
                for (int i = 0; i <= token.length() - 3; i++) {
                    String trigram = token.substring(i, i + 3);
                    int hash2 = Math.abs(trigram.hashCode() * 31) % dimension;
                    vector[hash2] += 0.5f;
                }
            }
        }

        VectorMath.l2Normalize(vector);

        // Memory protection for cache
        if (embeddingCache.size() > 10000) {
            embeddingCache.clear();
        }

        return vector;
    }

    private String hashContent(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }
}
