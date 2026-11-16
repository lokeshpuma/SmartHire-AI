package com.example.resumescreening.taxonomy;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "skills")
public class Skill {

    @Id
    private UUID id;

    @Column(name = "canonical_name", nullable = false, unique = true)
    private String canonicalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillCategory category;

    @Column(name = "taxonomy_version", nullable = false)
    private String taxonomyVersion = "1.0.0";

    @Column(columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SkillAlias> aliases = new ArrayList<>();

    public Skill() {
        this.id = UUID.randomUUID();
        this.taxonomyVersion = "1.0.0";
    }

    public Skill(String canonicalName, SkillCategory category, String description) {
        this();
        this.canonicalName = canonicalName;
        this.category = category;
        this.description = description;
    }

    public void addAlias(String aliasText) {
        SkillAlias alias = new SkillAlias(this, aliasText);
        aliases.add(alias);
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

    public List<SkillAlias> getAliases() { return aliases; }
    public void setAliases(List<SkillAlias> aliases) { this.aliases = aliases; }
}
