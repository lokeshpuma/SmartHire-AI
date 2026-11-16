package com.example.resumescreening.embedding;

public class VectorMath {

    private VectorMath() {}

    public static double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA == null || vecB == null || vecA.length == 0 || vecB.length == 0) {
            return 0.0;
        }
        if (vecA.length != vecB.length) {
            throw new IllegalArgumentException("Vector dimensions mismatch: " + vecA.length + " != " + vecB.length);
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }

        if (normA <= 1e-9 || normB <= 1e-9) {
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        // Bound between 0.0 and 1.0 for normalized text similarity
        return Math.max(0.0, Math.min(1.0, similarity));
    }

    public static void l2Normalize(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            sum += v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm > 1e-9) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
    }
}
