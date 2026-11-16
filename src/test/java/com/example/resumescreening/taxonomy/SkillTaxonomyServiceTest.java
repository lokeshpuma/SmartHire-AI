package com.example.resumescreening.taxonomy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillTaxonomyServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillAliasRepository skillAliasRepository;

    private SkillTaxonomyService taxonomyService;

    @BeforeEach
    void setUp() {
        taxonomyService = new SkillTaxonomyService(skillRepository, skillAliasRepository, "1.0.0");

        List<Skill> skills = new ArrayList<>();

        Skill java = new Skill("Java", SkillCategory.PROGRAMMING_LANGUAGE, "Java platform");
        java.addAlias("java 21");
        skills.add(java);

        Skill js = new Skill("JavaScript", SkillCategory.PROGRAMMING_LANGUAGE, "JavaScript");
        js.addAlias("js");
        js.addAlias("ecmascript");
        skills.add(js);

        Skill cpp = new Skill("C++", SkillCategory.PROGRAMMING_LANGUAGE, "C++ programming language");
        cpp.addAlias("cpp");
        skills.add(cpp);

        Skill c = new Skill("C", SkillCategory.PROGRAMMING_LANGUAGE, "C programming language");
        skills.add(c);

        Skill pg = new Skill("PostgreSQL", SkillCategory.DATABASE, "PostgreSQL database");
        pg.addAlias("postgres");
        pg.addAlias("psql");
        skills.add(pg);

        Skill k8s = new Skill("Kubernetes", SkillCategory.TOOL, "Kubernetes container orchestrator");
        k8s.addAlias("k8s");
        k8s.addAlias("kube");
        skills.add(k8s);

        when(skillRepository.findAllWithAliases()).thenReturn(skills);
        taxonomyService.refreshCache();
    }

    @Test
    @DisplayName("Normalizes alias JS to canonical JavaScript")
    void testNormalizeAliasJs() {
        Optional<Skill> result = taxonomyService.resolveSkill("js");
        assertTrue(result.isPresent());
        assertEquals("JavaScript", result.get().getCanonicalName());
    }

    @Test
    @DisplayName("Normalizes alias postgres to canonical PostgreSQL")
    void testNormalizeAliasPostgres() {
        Optional<Skill> result = taxonomyService.resolveSkill("postgres");
        assertTrue(result.isPresent());
        assertEquals("PostgreSQL", result.get().getCanonicalName());
    }

    @Test
    @DisplayName("Distinguishes C++ from C without false merging")
    void testDistinguishCppAndC() {
        Optional<Skill> cppResult = taxonomyService.resolveSkill("C++");
        assertTrue(cppResult.isPresent());
        assertEquals("C++", cppResult.get().getCanonicalName());

        Optional<Skill> cResult = taxonomyService.resolveSkill("C");
        assertTrue(cResult.isPresent());
        assertEquals("C", cResult.get().getCanonicalName());

        assertNotEquals(cppResult.get().getCanonicalName(), cResult.get().getCanonicalName());
    }

    @Test
    @DisplayName("Extracts multiple canonical skills from free text")
    void testExtractSkillsFromText() {
        String bio = "Experienced in C++, Java 21, and deploying microservices on k8s with postgres.";
        List<SkillTaxonomyService.SkillMatch> matches = taxonomyService.extractSkillsFromText(bio);

        List<String> canonicalNames = matches.stream().map(m -> m.getSkill().getCanonicalName()).toList();
        assertTrue(canonicalNames.contains("C++"));
        assertTrue(canonicalNames.contains("Java"));
        assertTrue(canonicalNames.contains("Kubernetes"));
        assertTrue(canonicalNames.contains("PostgreSQL"));
        // Ensure "C" is not incorrectly matched when "C++" was intended
        assertFalse(canonicalNames.contains("C"));
    }
}
