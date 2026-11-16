package com.example.resumescreening.extraction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeProfileRepository extends JpaRepository<ResumeProfile, UUID> {
    Optional<ResumeProfile> findByCandidateId(UUID candidateId);
    Optional<ResumeProfile> findByResumeDocumentId(UUID documentId);

    @Query("SELECT p FROM ResumeProfile p LEFT JOIN FETCH p.evidenceItems WHERE p.candidate.id = :candidateId")
    Optional<ResumeProfile> findByCandidateIdWithEvidence(UUID candidateId);
}
