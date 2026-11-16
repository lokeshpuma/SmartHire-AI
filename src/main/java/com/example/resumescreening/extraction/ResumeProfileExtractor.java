package com.example.resumescreening.extraction;

import com.example.resumescreening.candidate.Candidate;
import com.example.resumescreening.document.DocumentTextExtractor;
import com.example.resumescreening.document.ResumeDocument;
import com.example.resumescreening.taxonomy.Skill;
import com.example.resumescreening.taxonomy.SkillTaxonomyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeProfileExtractor {

    private static final Logger log = LoggerFactory.getLogger(ResumeProfileExtractor.class);
    private static final String EXTRACTION_VERSION = "2.1.0-explainable";

    private final SkillTaxonomyService taxonomyService;
    private final ProfileClassifierService classifierService;
    private final ObjectMapper objectMapper;

    public ResumeProfileExtractor(SkillTaxonomyService taxonomyService,
                                  ProfileClassifierService classifierService,
                                  ObjectMapper objectMapper) {
        this.taxonomyService = taxonomyService;
        this.classifierService = classifierService;
        this.objectMapper = objectMapper;
    }

    public ResumeProfile extractProfile(Candidate candidate, ResumeDocument document, DocumentTextExtractor.ExtractionResult extractionResult) {
        ResumeProfile profile = new ResumeProfile(
                candidate,
                document,
                extractionResult.getNormalizedFullText(),
                "{}",
                EXTRACTION_VERSION
        );

        List<DocumentTextExtractor.PageText> pages = extractionResult.getPages();
        Map<String, Object> structuredData = new LinkedHashMap<>();

        // 1. Extract Sections (Summary, Experience, Education, Skills, Certifications, Projects)
        Map<String, String> sections = extractSections(extractionResult.getNormalizedFullText());

        // 2. Extract Skills with Evidence (exact text snippet, page number, confidence)
        Set<String> seenCanonicalSkills = new HashSet<>();
        List<Skill> matchedSkills = new ArrayList<>();
        List<Map<String, Object>> skillsJson = new ArrayList<>();

        for (DocumentTextExtractor.PageText page : pages) {
            List<SkillTaxonomyService.SkillMatch> matches = taxonomyService.extractSkillsFromText(page.getText());
            for (SkillTaxonomyService.SkillMatch match : matches) {
                Skill skill = match.getSkill();
                if (seenCanonicalSkills.add(skill.getCanonicalName())) {
                    matchedSkills.add(skill);

                    // Extract context snippet around match
                    String snippet = extractSnippet(page.getText(), match.getStartOffset(), match.getEndOffset());

                    FieldType fieldType = mapCategoryToFieldType(skill);
                    ExtractedEvidence evidence = new ExtractedEvidence(
                            profile,
                            fieldType,
                            skill.getCanonicalName(),
                            snippet,
                            page.getPageNumber(),
                            match.getStartOffset()
                    );
                    evidence.setExtractionMethod(ExtractionMethod.RULE_BASED);
                    evidence.setConfidence(0.95);
                    evidence.setReviewStatus(EvidenceReviewStatus.UNVERIFIED);
                    evidence.setNotes("Detected via canonical taxonomy match: '" + match.getMatchedText() + "'");
                    profile.addEvidence(evidence);

                    Map<String, Object> skillEntry = new LinkedHashMap<>();
                    skillEntry.put("skill", skill.getCanonicalName());
                    skillEntry.put("category", skill.getCategory().name());
                    skillEntry.put("page", page.getPageNumber());
                    skillEntry.put("snippet", snippet);
                    skillsJson.add(skillEntry);
                }
            }
        }
        structuredData.put("skills", skillsJson);

        // 3. Extract Experience
        List<Map<String, Object>> experienceJson = extractExperienceEntries(sections.getOrDefault("EXPERIENCE", extractionResult.getNormalizedFullText()), profile);
        structuredData.put("experience", experienceJson);

        // 4. Extract Education (explicitly labeled for human review, excluded from scoring)
        List<Map<String, Object>> educationJson = extractEducationEntries(sections.getOrDefault("EDUCATION", extractionResult.getNormalizedFullText()), profile);
        structuredData.put("education", educationJson);

        // 5. Extract Certifications
        List<Map<String, Object>> certsJson = extractCertifications(sections.getOrDefault("CERTIFICATIONS", extractionResult.getNormalizedFullText()), profile);
        structuredData.put("certifications", certsJson);

        // 6. Treat missing sections as UNKNOWN
        Map<String, String> unknowns = new LinkedHashMap<>();
        if (!sections.containsKey("EDUCATION") || educationJson.isEmpty()) {
            unknowns.put("education", "No explicit education section identified; treated as unknown");
        }
        if (!sections.containsKey("EXPERIENCE") || experienceJson.isEmpty()) {
            unknowns.put("experience", "No structured experience section identified; treated as unknown");
        }
        if (certsJson.isEmpty()) {
            unknowns.put("certifications", "No certifications identified; treated as unknown");
        }
        structuredData.put("unknown_fields", unknowns);

        // 7. Profile Advisory Classification
        ProfileClassifierService.ClassificationResult classification = classifierService.classify(matchedSkills, extractionResult.getNormalizedFullText());
        profile.setClassificationLabel(classification.getCategory());
        profile.setClassificationConfidence(classification.getConfidence());
        structuredData.put("advisory_classification", Map.of(
                "label", classification.getCategory(),
                "confidence", classification.getConfidence(),
                "evidence", classification.getMainEvidence(),
                "note", "Advisory classification only. Must not be used for automatic exclusion."
        ));

        try {
            profile.setProfileJson(objectMapper.writeValueAsString(structuredData));
        } catch (Exception e) {
            log.error("Failed to serialize profile JSON", e);
            profile.setProfileJson("{}");
        }

        return profile;
    }

    private FieldType mapCategoryToFieldType(Skill skill) {
        switch (skill.getCategory()) {
            case TOOL:
                return FieldType.TOOL;
            case DOMAIN:
                return FieldType.DOMAIN_TERM;
            default:
                return FieldType.SKILL;
        }
    }

    private String extractSnippet(String text, int start, int end) {
        int snippetStart = Math.max(0, start - 50);
        int snippetEnd = Math.min(text.length(), end + 50);
        String sub = text.substring(snippetStart, snippetEnd).replaceAll("\\s+", " ").trim();
        return (snippetStart > 0 ? "..." : "") + sub + (snippetEnd < text.length() ? "..." : "");
    }

    private Map<String, String> extractSections(String text) {
        Map<String, String> sections = new HashMap<>();
        String[] lines = text.split("\\r?\\n");
        String currentSection = "HEADER";
        StringBuilder currentContent = new StringBuilder();

        Pattern sectionPattern = Pattern.compile("(?i)^(summary|objective|experience|work history|employment|education|skills|technical skills|certifications?|projects?)\\b");

        for (String line : lines) {
            String trimmed = line.trim();
            Matcher matcher = sectionPattern.matcher(trimmed);
            if (matcher.find()) {
                if (currentContent.length() > 0) {
                    sections.put(currentSection, currentContent.toString());
                    currentContent.setLength(0);
                }
                String matchedName = matcher.group(1).toUpperCase();
                if (matchedName.contains("EXPERIENCE") || matchedName.contains("EMPLOYMENT") || matchedName.contains("WORK")) {
                    currentSection = "EXPERIENCE";
                } else if (matchedName.contains("EDUCATION")) {
                    currentSection = "EDUCATION";
                } else if (matchedName.contains("SKILL")) {
                    currentSection = "SKILLS";
                } else if (matchedName.contains("CERT")) {
                    currentSection = "CERTIFICATIONS";
                } else if (matchedName.contains("PROJECT")) {
                    currentSection = "PROJECTS";
                } else {
                    currentSection = "SUMMARY";
                }
            } else {
                currentContent.append(line).append("\n");
            }
        }

        if (currentContent.length() > 0) {
            sections.put(currentSection, currentContent.toString());
        }

        return sections;
    }

    private List<Map<String, Object>> extractExperienceEntries(String expText, ResumeProfile profile) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (expText == null || expText.isBlank()) return result;

        // Date range pattern e.g. "2020 - 2023", "Jan 2021 - Present", "2019 - Present"
        Pattern datePattern = Pattern.compile("(?i)(\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)?\\s*\\d{4})\\s*(?:-|–|to)\\s*(\\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)?\\s*\\d{4}|present)\\b");
        Matcher matcher = datePattern.matcher(expText);

        while (matcher.find()) {
            String startStr = matcher.group(1).trim();
            String endStr = matcher.group(2).trim();

            int startIdx = Math.max(0, matcher.start() - 80);
            int endIdx = Math.min(expText.length(), matcher.end() + 150);
            String context = expText.substring(startIdx, endIdx).replaceAll("\\s+", " ").trim();

            Map<String, Object> exp = new LinkedHashMap<>();
            exp.put("period", matcher.group());
            exp.put("start", startStr);
            exp.put("end", endStr);
            exp.put("context", context);
            exp.put("duration_certainty", "ESTIMATED_FROM_DATES");

            result.add(exp);

            ExtractedEvidence evidence = new ExtractedEvidence(
                    profile,
                    FieldType.EXPERIENCE,
                    "Experience: " + matcher.group(),
                    context,
                    1,
                    matcher.start()
            );
            evidence.setNotes("Employment period identified. Duration verification requires recruiter inspection.");
            profile.addEvidence(evidence);
        }

        return result;
    }

    private List<Map<String, Object>> extractEducationEntries(String eduText, ResumeProfile profile) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (eduText == null || eduText.isBlank()) return result;

        Pattern degreePattern = Pattern.compile("(?i)\\b(bachelor|master|b\\.s|m\\.s|b\\.tech|m\\.tech|phd|associate|degree)\\b[^\\n.]+");
        Matcher matcher = degreePattern.matcher(eduText);

        while (matcher.find()) {
            String matchStr = matcher.group().trim();
            Map<String, Object> edu = new LinkedHashMap<>();
            edu.put("degree", matchStr);
            edu.put("scored_in_ranking", false); // EXCLUDED from scoring per Section 6.2
            result.add(edu);

            ExtractedEvidence evidence = new ExtractedEvidence(
                    profile,
                    FieldType.EDUCATION,
                    matchStr,
                    matchStr,
                    1,
                    matcher.start()
            );
            evidence.setNotes("Education evidence stored for human review only. Excluded from scoring features.");
            profile.addEvidence(evidence);
        }

        return result;
    }

    private List<Map<String, Object>> extractCertifications(String certText, ResumeProfile profile) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (certText == null || certText.isBlank()) return result;

        Pattern certPattern = Pattern.compile("(?i)\\b(certified|certification|aws certified|azure certified|cka|ckad|pmp|scrum master)\\b[^\\n.]+");
        Matcher matcher = certPattern.matcher(certText);

        while (matcher.find()) {
            String matchStr = matcher.group().trim();
            Map<String, Object> cert = new LinkedHashMap<>();
            cert.put("name", matchStr);
            result.add(cert);

            ExtractedEvidence evidence = new ExtractedEvidence(
                    profile,
                    FieldType.CERTIFICATION,
                    matchStr,
                    matchStr,
                    1,
                    matcher.start()
            );
            profile.addEvidence(evidence);
        }

        return result;
    }
}
