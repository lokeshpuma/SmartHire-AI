package com.example.resumescreening.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeDocumentRepository extends JpaRepository<ResumeDocument, UUID> {
    Optional<ResumeDocument> findByCandidateIdAndDeletedAtIsNull(UUID candidateId);
    List<ResumeDocument> findByCandidateId(UUID candidateId);

    @Query("SELECT d FROM ResumeDocument d JOIN FETCH d.candidate WHERE d.fileHash = :fileHash AND d.deletedAt IS NULL")
    Optional<ResumeDocument> findByFileHashAndDeletedAtIsNull(String fileHash);
}
