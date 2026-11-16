package com.example.resumescreening.taxonomy;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "skill_aliases")
public class SkillAlias {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private String alias;

    public SkillAlias() {
        this.id = UUID.randomUUID();
    }

    public SkillAlias(Skill skill, String alias) {
        this();
        this.skill = skill;
        this.alias = alias;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
}
