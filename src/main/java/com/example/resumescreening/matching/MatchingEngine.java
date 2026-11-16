package com.example.resumescreening.matching;

import com.example.resumescreening.embedding.EmbeddingService;
import com.example.resumescreening.extraction.ExtractedEvidence;
import com.example.resumescreening.extraction.ResumeProfile;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobRequirement;
import com.example.resumescreening.job.RequirementType;
import com.example.resumescreening.taxonomy.SkillTaxonomyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);
    private static final String SCORING_CONFIG_VERSION = "1.2.0-transparent";

    private final EmbeddingService embeddingService;
    private final SkillTaxonomyService taxonomyService;
    private final ObjectMapper objectMapper;

    private final double weightRequired;
    private final double weightPreferred;
    private final double weightSemantic;
    private final double weightEvidence;

    public MatchingEngine(
            EmbeddingService embeddingService,
            SkillTaxonomyService taxonomyService,
            ObjectMapper objectMapper,
            @Value("${app.matching.weights.required:0.45}") double weightRequired,
            @Value("${app.matching.weights.preferred:0.20}") double weightPreferred,
            @Value("${app.matching.weights.semantic:0.20}") double weightSemantic,
            @Value("${app.matching.weights.evidence:0.15}") double weightEvidence) {
        this.embeddingService = embeddingService;
        this.taxonomyService = taxonomyService;
        this.objectMapper = objectMapper;

        // Ensure weights are normalized to sum to 1.0
        double total = weightRequired + weightPreferred + weightSemantic + weightEvidence;
        if (total <= 0) total = 1.0;
        this.weightRequired = weightRequired / total;
        this.weightPreferred = weightPreferred / total;
        this.weightSemantic = weightSemantic / total;
        this.weightEvidence = weightEvidence / total;

        log.info("Initialized MatchingEngine with normalized weights: req={}, pref={}, sem={}, ev={}",
                this.weightRequired, this.weightPreferred, this.weightSemantic, this.weightEvidence);
    }

    public CandidateJobAssessment assessCandidate(Job job, ResumeProfile profile) {
        List<JobRequirement> requirements = job.getRequirements() != null ? job.getRequirements() : Collections.emptyList();
        List<ExtractedEvidence> candidateEvidence = profile.getEvidenceItems() != null ? profile.getEvidenceItems() : Collections.emptyList();

        // Map candidate evidence canonical values
        Map<String, List<ExtractedEvidence>> evidenceByCanonical = new HashMap<>();
        for (ExtractedEvidence ev : candidateEvidence) {
            String key = ev.getCanonicalValue().toLowerCase();
            evidenceByCanonical.computeIfAbsent(key, k -> new ArrayList<>()).add(ev);
        }

        List<RequirementEvaluation> evaluations = new ArrayList<>();
        double requiredMatchedWeight = 0.0;
        double requiredTotalWeight = 0.0;
        double preferredMatchedWeight = 0.0;
        double preferredTotalWeight = 0.0;

        List<Map<String, Object>> matchedEvidenceList = new ArrayList<>();
        List<Map<String, Object>> missingOrUnknownList = new ArrayList<>();

        for (JobRequirement req : requirements) {
            if (!req.isActive()) continue;

            RequirementEvaluation eval = evaluateRequirement(req, evidenceByCanonical, profile.getNormalizedText());
            evaluations.add(eval);

            if (req.getRequirementType() == RequirementType.REQUIRED) {
                requiredTotalWeight += req.getWeight();
                if (eval.isMatched()) {
                    requiredMatchedWeight += req.getWeight() * eval.getMatchQuality();
                }
            } else if (req.getRequirementType() == RequirementType.PREFERRED) {
                preferredTotalWeight += req.getWeight();
                if (eval.isMatched()) {
                    preferredMatchedWeight += req.getWeight() * eval.getMatchQuality();
                }
            }

            if (eval.isMatched()) {
                Map<String, Object> matchInfo = new LinkedHashMap<>();
                matchInfo.put("requirementId", req.getId() != null ? req.getId().toString() : "");
                matchInfo.put("skill", req.getCanonicalSkillId());
                matchInfo.put("requirementText", req.getRequirementText());
                matchInfo.put("type", req.getRequirementType().name());
                matchInfo.put("matchQuality", eval.getMatchQuality());
                matchInfo.put("evidenceSnippets", eval.getEvidenceSnippets());
                matchedEvidenceList.add(matchInfo);
            } else {
                Map<String, Object> missInfo = new LinkedHashMap<>();
                missInfo.put("requirementId", req.getId() != null ? req.getId().toString() : "");
                missInfo.put("skill", req.getCanonicalSkillId());
                missInfo.put("requirementText", req.getRequirementText());
                missInfo.put("type", req.getRequirementType().name());
                missInfo.put("status", "NO_LOCATED_EVIDENCE");
                missInfo.put("interpretation", "Treated as unknown in candidate profile; not an absolute disqualifier.");
                missingOrUnknownList.add(missInfo);
            }
        }

        double requiredCoverage = requiredTotalWeight > 0 ? (requiredMatchedWeight / requiredTotalWeight) : 1.0;
        double preferredCoverage = preferredTotalWeight > 0 ? (preferredMatchedWeight / preferredTotalWeight) : 1.0;

        // Semantic similarity between job description and resume normalized text
        double semanticRelevance = embeddingService.calculateSimilarity(
                job.getDescription(),
                profile.getNormalizedText()
        );

        // Evidence coverage: fraction of requirements that have verified or direct text quotes
        long totalActive = evaluations.size();
        long withDirectEvidence = evaluations.stream().filter(e -> !e.getEvidenceSnippets().isEmpty()).count();
        double evidenceCoverage = totalActive > 0 ? ((double) withDirectEvidence / totalActive) : 1.0;

        // Explainable Composite Match Score
        double overallScore = (requiredCoverage * weightRequired)
                + (preferredCoverage * weightPreferred)
                + (semanticRelevance * weightSemantic)
                + (evidenceCoverage * weightEvidence);
        overallScore = Math.max(0.0, Math.min(1.0, overallScore));

        // Data completeness / confidence calculated SEPARATELY per Section 4.8
        double completeness = calculateDataCompleteness(profile, evaluations.size());

        CandidateJobAssessment assessment = new CandidateJobAssessment();
        assessment.setCandidate(profile.getCandidate());
        assessment.setJob(job);
        assessment.setOverallScore(Math.round(overallScore * 1000.0) / 1000.0);
        assessment.setConfidenceOrCompleteness(Math.round(completeness * 1000.0) / 1000.0);
        assessment.setRequiredCoverage(Math.round(requiredCoverage * 1000.0) / 1000.0);
        assessment.setPreferredCoverage(Math.round(preferredCoverage * 1000.0) / 1000.0);
        assessment.setSemanticRelevance(Math.round(semanticRelevance * 1000.0) / 1000.0);
        assessment.setEvidenceCoverage(Math.round(evidenceCoverage * 1000.0) / 1000.0);
        assessment.setEmbeddingModelVersion(embeddingService.getModelName() + ":" + embeddingService.getModelVersion());
        assessment.setScoringConfigVersion(SCORING_CONFIG_VERSION);

        // Detailed score breakdown JSON
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("formula", "score = (required * " + weightRequired + ") + (preferred * " + weightPreferred + ") + (semantic * " + weightSemantic + ") + (evidence * " + weightEvidence + ")");
        breakdown.put("weights", Map.of(
                "required", weightRequired,
                "preferred", weightPreferred,
                "semantic", weightSemantic,
                "evidence", weightEvidence
        ));
        breakdown.put("components", Map.of(
                "requiredCoverage", requiredCoverage,
                "preferredCoverage", preferredCoverage,
                "semanticRelevance", semanticRelevance,
                "evidenceCoverage", evidenceCoverage
        ));
        breakdown.put("evaluations", evaluations);

        try {
            assessment.setScoreBreakdownJson(objectMapper.writeValueAsString(breakdown));
            assessment.setMatchedEvidenceJson(objectMapper.writeValueAsString(matchedEvidenceList));
            assessment.setMissingOrUnknownRequirementsJson(objectMapper.writeValueAsString(missingOrUnknownList));
        } catch (Exception e) {
            log.error("Failed to serialize assessment breakdown", e);
            assessment.setScoreBreakdownJson("{}");
            assessment.setMatchedEvidenceJson("[]");
            assessment.setMissingOrUnknownRequirementsJson("[]");
        }

        return assessment;
    }

    private RequirementEvaluation evaluateRequirement(JobRequirement req, Map<String, List<ExtractedEvidence>> evidenceByCanonical, String fullText) {
        String canonicalKey = req.getCanonicalSkillId() != null ? req.getCanonicalSkillId().toLowerCase() : "";
        List<ExtractedEvidence> exactEvidences = evidenceByCanonical.getOrDefault(canonicalKey, Collections.emptyList());

        List<String> evidenceSnippets = new ArrayList<>();
        for (ExtractedEvidence ev : exactEvidences) {
            evidenceSnippets.add("[Page " + ev.getPageNumber() + "] " + ev.getOriginalText());
        }

        if (!exactEvidences.isEmpty()) {
            return new RequirementEvaluation(req.getCanonicalSkillId(), true, 1.0, evidenceSnippets, "EXACT_CANONICAL_MATCH");
        }

        // Check acceptable synonyms
        if (req.getAcceptableSynonyms() != null && !req.getAcceptableSynonyms().isBlank()) {
            String[] synonyms = req.getAcceptableSynonyms().split(",");
            for (String syn : synonyms) {
                String synKey = syn.trim().toLowerCase();
                List<ExtractedEvidence> synEvidences = evidenceByCanonical.getOrDefault(synKey, Collections.emptyList());
                if (!synEvidences.isEmpty()) {
                    for (ExtractedEvidence ev : synEvidences) {
                        evidenceSnippets.add("[Page " + ev.getPageNumber() + "] " + ev.getOriginalText());
                    }
                    return new RequirementEvaluation(req.getCanonicalSkillId(), true, 0.95, evidenceSnippets, "SYNONYM_MATCH: " + syn.trim());
                }
            }
        }

        // Semantic embedding similarity check for requirement text against resume passages
        double sim = embeddingService.calculateSimilarity(req.getRequirementText(), fullText);
        if (sim >= 0.70) {
            return new RequirementEvaluation(req.getCanonicalSkillId(), true, Math.min(0.85, sim), List.of("Semantically matched context in resume (similarity: " + String.format("%.2f", sim) + ")"), "SEMANTIC_PASSAGE_MATCH");
        }

        return new RequirementEvaluation(req.getCanonicalSkillId(), false, 0.0, Collections.emptyList(), "NO_EVIDENCE_FOUND");
    }

    private double calculateDataCompleteness(ResumeProfile profile, int requirementCount) {
        double score = 0.30; // baseline for valid document extraction
        if (profile.getEvidenceItems() != null && !profile.getEvidenceItems().isEmpty()) {
            score += Math.min(0.40, profile.getEvidenceItems().size() * 0.08);
        }
        if (profile.getNormalizedText() != null && profile.getNormalizedText().length() > 50) {
            score += 0.15;
        }
        if (profile.getClassificationConfidence() > 0.5) {
            score += 0.15;
        }
        return Math.min(1.0, Math.round(score * 100.0) / 100.0);
    }

    public static class RequirementEvaluation {
        private final String requirementKey;
        private final boolean matched;
        private final double matchQuality;
        private final List<String> evidenceSnippets;
        private final String matchType;

        public RequirementEvaluation(String requirementKey, boolean matched, double matchQuality, List<String> evidenceSnippets, String matchType) {
            this.requirementKey = requirementKey;
            this.matched = matched;
            this.matchQuality = matchQuality;
            this.evidenceSnippets = evidenceSnippets;
            this.matchType = matchType;
        }

        public String getRequirementKey() { return requirementKey; }
        public boolean isMatched() { return matched; }
        public double getMatchQuality() { return matchQuality; }
        public List<String> getEvidenceSnippets() { return evidenceSnippets; }
        public String getMatchType() { return matchType; }
    }
}
