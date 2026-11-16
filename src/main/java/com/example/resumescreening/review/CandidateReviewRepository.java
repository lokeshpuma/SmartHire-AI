package com.example.resumescreening.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateReviewRepository extends JpaRepository<CandidateReview, UUID> {
    List<CandidateReview> findByJobId(UUID jobId);
    List<CandidateReview> findByCandidateId(UUID candidateId);
    Optional<CandidateReview> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);
    void deleteByCandidateId(UUID candidateId);
}
