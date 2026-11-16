package com.example.resumescreening.taxonomy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByCanonicalNameIgnoreCase(String canonicalName);

    @Query("SELECT DISTINCT s FROM Skill s LEFT JOIN FETCH s.aliases")
    List<Skill> findAllWithAliases();
}
