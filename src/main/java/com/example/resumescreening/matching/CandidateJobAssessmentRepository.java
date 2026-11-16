package com.example.resumescreening.matching;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateJobAssessmentRepository extends JpaRepository<CandidateJobAssessment, UUID> {
    @Query("SELECT a FROM CandidateJobAssessment a JOIN FETCH a.candidate JOIN FETCH a.job WHERE a.job.id = :jobId ORDER BY a.overallScore DESC")
    List<CandidateJobAssessment> findByJobIdOrderByOverallScoreDesc(UUID jobId);

    @Query(value = "SELECT a FROM CandidateJobAssessment a JOIN FETCH a.candidate JOIN FETCH a.job WHERE a.job.id = :jobId ORDER BY a.overallScore DESC",
           countQuery = "SELECT COUNT(a) FROM CandidateJobAssessment a WHERE a.job.id = :jobId")
    Page<CandidateJobAssessment> findByJobIdOrderByOverallScoreDesc(UUID jobId, Pageable pageable);

    @Query("SELECT a FROM CandidateJobAssessment a JOIN FETCH a.candidate JOIN FETCH a.job WHERE a.job.id = :jobId AND a.candidate.id = :candidateId")
    Optional<CandidateJobAssessment> findByJobIdAndCandidateId(UUID jobId, UUID candidateId);

    @Query("SELECT a FROM CandidateJobAssessment a JOIN FETCH a.candidate JOIN FETCH a.job WHERE a.candidate.id = :candidateId")
    List<CandidateJobAssessment> findByCandidateId(UUID candidateId);

    void deleteByCandidateId(UUID candidateId);
    long countByJobId(UUID jobId);
}
