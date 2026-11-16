package com.example.resumescreening.extraction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtractedEvidenceRepository extends JpaRepository<ExtractedEvidence, UUID> {
    List<ExtractedEvidence> findByResumeProfileId(UUID resumeProfileId);
    Optional<ExtractedEvidence> findByIdAndResumeProfileId(UUID id, UUID resumeProfileId);
}
