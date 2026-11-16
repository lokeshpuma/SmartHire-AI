package com.example.resumescreening.embedding;

import java.util.Arrays;

public class EmbeddingResult {
    private final float[] vector;
    private final int dimension;
    private final String modelName;
    private final String modelVersion;

    public EmbeddingResult(float[] vector, int dimension, String modelName, String modelVersion) {
        this.vector = vector;
        this.dimension = dimension;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
    }

    public float[] getVector() { return vector; }
    public int getDimension() { return dimension; }
    public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; }

    @Override
    public String toString() {
        return "EmbeddingResult{" +
                "dimension=" + dimension +
                ", modelName='" + modelName + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                '}';
    }
}
