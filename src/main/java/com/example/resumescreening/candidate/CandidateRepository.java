package com.example.resumescreening.candidate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
    Page<Candidate> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);
    Optional<Candidate> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    Optional<Candidate> findByWorkspaceIdAndCandidateReference(UUID workspaceId, String candidateReference);
    Optional<Candidate> findByCandidateReference(String candidateReference);
    Optional<Candidate> findFirstByEmailOrderByCreatedAtDesc(String email);
    long countByWorkspaceId(UUID workspaceId);
}
