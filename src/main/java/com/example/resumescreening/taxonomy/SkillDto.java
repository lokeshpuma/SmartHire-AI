package com.example.resumescreening.taxonomy;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SkillDto {
    private UUID id;
    private String canonicalName;
    private SkillCategory category;
    private String taxonomyVersion;
    private String description;
    private List<String> aliases;

    public SkillDto() {}

    public SkillDto(UUID id, String canonicalName, SkillCategory category, String taxonomyVersion, String description, List<String> aliases) {
        this.id = id;
        this.canonicalName = canonicalName;
        this.category = category;
        this.taxonomyVersion = taxonomyVersion;
        this.description = description;
        this.aliases = aliases;
    }

    public static SkillDto fromEntity(Skill skill) {
        return new SkillDto(
                skill.getId(),
                skill.getCanonicalName(),
                skill.getCategory(),
                skill.getTaxonomyVersion(),
                skill.getDescription(),
                skill.getAliases() != null ? skill.getAliases().stream().map(SkillAlias::getAlias).collect(Collectors.toList()) : List.of()
        );
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCanonicalName() { return canonicalName; }
    public void setCanonicalName(String canonicalName) { this.canonicalName = canonicalName; }

    public SkillCategory getCategory() { return category; }
    public void setCategory(SkillCategory category) { this.category = category; }

    public String getTaxonomyVersion() { return taxonomyVersion; }
    public void setTaxonomyVersion(String taxonomyVersion) { this.taxonomyVersion = taxonomyVersion; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getAliases() { return aliases; }
    public void setAliases(List<String> aliases) { this.aliases = aliases; }
}
