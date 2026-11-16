-- Intelligent Resume Screening Engine Database Schema
-- Version 1.0.0

CREATE TABLE IF NOT EXISTS workspaces (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    settings_json TEXT,
    retention_policy VARCHAR(255) DEFAULT 'standard-365-days',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    department VARCHAR(255),
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_requirements (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    canonical_skill_id VARCHAR(255),
    requirement_text TEXT NOT NULL,
    requirement_type VARCHAR(50) NOT NULL DEFAULT 'REQUIRED',
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    acceptable_synonyms TEXT,
    evidence_guidance TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS candidates (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    candidate_reference VARCHAR(100) NOT NULL,
    display_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resume_documents (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    storage_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    extraction_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    parser_version VARCHAR(50),
    error_message TEXT,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS resume_profiles (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    resume_document_id UUID NOT NULL REFERENCES resume_documents(id) ON DELETE CASCADE,
    normalized_text TEXT,
    profile_json TEXT,
    extraction_version VARCHAR(50),
    classification_label VARCHAR(100),
    classification_confidence DOUBLE PRECISION,
    review_status VARCHAR(50) NOT NULL DEFAULT 'NOT_REVIEWED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS extracted_evidence (
    id UUID PRIMARY KEY,
    resume_profile_id UUID NOT NULL REFERENCES resume_profiles(id) ON DELETE CASCADE,
    field_type VARCHAR(50) NOT NULL,
    canonical_value VARCHAR(255) NOT NULL,
    original_text TEXT,
    page_number INTEGER,
    text_offset INTEGER,
    extraction_method VARCHAR(50) NOT NULL DEFAULT 'RULE_BASED',
    confidence DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    review_status VARCHAR(50) NOT NULL DEFAULT 'UNVERIFIED',
    notes TEXT
);

CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY,
    canonical_name VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL,
    taxonomy_version VARCHAR(50) NOT NULL DEFAULT '1.0.0',
    description TEXT
);

CREATE TABLE IF NOT EXISTS skill_aliases (
    id UUID PRIMARY KEY,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    alias VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS candidate_job_assessments (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    overall_score DOUBLE PRECISION NOT NULL,
    confidence_or_completeness DOUBLE PRECISION NOT NULL,
    required_coverage DOUBLE PRECISION NOT NULL,
    preferred_coverage DOUBLE PRECISION NOT NULL,
    semantic_relevance DOUBLE PRECISION NOT NULL,
    evidence_coverage DOUBLE PRECISION NOT NULL,
    score_breakdown_json TEXT,
    matched_evidence_json TEXT,
    missing_or_unknown_requirements_json TEXT,
    embedding_model_version VARCHAR(100),
    scoring_config_version VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS candidate_reviews (
    id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    reviewer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    review_status VARCHAR(50) NOT NULL DEFAULT 'UNDER_REVIEW',
    decision VARCHAR(50),
    reason VARCHAR(255),
    notes TEXT,
    requested_reviewer VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    actor_id UUID,
    actor_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    metadata_redacted_json TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance & workspace isolation
CREATE INDEX IF NOT EXISTS idx_users_workspace ON users(workspace_id);
CREATE INDEX IF NOT EXISTS idx_jobs_workspace ON jobs(workspace_id);
CREATE INDEX IF NOT EXISTS idx_candidates_workspace ON candidates(workspace_id);
CREATE INDEX IF NOT EXISTS idx_resume_documents_hash ON resume_documents(file_hash);
CREATE INDEX IF NOT EXISTS idx_assessments_job_score ON candidate_job_assessments(job_id, overall_score DESC);
CREATE INDEX IF NOT EXISTS idx_reviews_candidate_job ON candidate_reviews(candidate_id, job_id);
CREATE INDEX IF NOT EXISTS idx_audit_workspace_timestamp ON audit_events(workspace_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_skill_aliases_alias ON skill_aliases(alias);
