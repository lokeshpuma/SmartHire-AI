package com.example.resumescreening.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    Page<Job> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);
    List<Job> findByWorkspaceId(UUID workspaceId);

    @Query("SELECT DISTINCT j FROM Job j LEFT JOIN FETCH j.requirements WHERE j.id = :id AND j.workspace.id = :workspaceId")
    Optional<Job> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
}
