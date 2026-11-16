package com.example.resumescreening.extraction;

import com.example.resumescreening.taxonomy.Skill;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProfileClassifierService {

    public static class ClassificationResult {
        private final String category;
        private final double confidence;
        private final String mainEvidence;

        public ClassificationResult(String category, double confidence, String mainEvidence) {
            this.category = category;
            this.confidence = confidence;
            this.mainEvidence = mainEvidence;
        }

        public String getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public String getMainEvidence() { return mainEvidence; }
    }

    public ClassificationResult classify(List<Skill> matchedSkills, String fullText) {
        if (matchedSkills == null || matchedSkills.isEmpty()) {
            return new ClassificationResult("General Software / Technical", 0.50, "General technical indicators");
        }

        Map<String, Integer> categoryScores = new HashMap<>();
        Map<String, List<String>> categoryEvidence = new HashMap<>();

        for (Skill skill : matchedSkills) {
            String cat = mapSkillToProfileCategory(skill);
            categoryScores.put(cat, categoryScores.getOrDefault(cat, 0) + 1);
            categoryEvidence.computeIfAbsent(cat, k -> new ArrayList<>()).add(skill.getCanonicalName());
        }

        // Find primary category
        String bestCategory = "Software Engineering";
        int maxScore = 0;
        for (Map.Entry<String, Integer> entry : categoryScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        double confidence = Math.min(0.95, 0.40 + (maxScore * 0.10));
        List<String> evidence = categoryEvidence.getOrDefault(bestCategory, List.of());
        String evidenceStr = "Supporting skills: " + String.join(", ", evidence.subList(0, Math.min(evidence.size(), 5)));

        return new ClassificationResult(bestCategory, confidence, evidenceStr);
    }

    private String mapSkillToProfileCategory(Skill skill) {
        String name = skill.getCanonicalName().toLowerCase();
        switch (skill.getCategory()) {
            case DOMAIN:
                if (name.contains("machine learning") || name.contains("nlp") || name.contains("vision")) {
                    return "Machine Learning / AI Engineering";
                }
                return "Software Engineering";
            case TESTING:
                return "Quality Engineering";
            case CLOUD_PLATFORM:
                return "DevOps / Platform Engineering";
            case TOOL:
                if (name.contains("docker") || name.contains("kubernetes") || name.contains("terraform") || name.contains("ansible")) {
                    return "DevOps / Platform Engineering";
                }
                if (name.contains("spark") || name.contains("pandas") || name.contains("numpy")) {
                    return "Data Engineering";
                }
                return "Software Engineering";
            case DATABASE:
                return "Data Engineering";
            case PROGRAMMING_LANGUAGE:
                if (name.equals("python")) {
                    return "Software Engineering";
                }
                return "Software Engineering";
            default:
                return "Software Engineering";
        }
    }
}
