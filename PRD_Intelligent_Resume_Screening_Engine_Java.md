# PRD.md — Intelligent Resume Screening Engine

## 1. Project Overview

**Project name:** Intelligent Resume Screening Engine  
**Category:** AI-Powered Recruitment / Resume Intelligence  
**Primary language:** Java 21  
**Backend framework:** Spring Boot  
**Core capabilities:** Resume parsing, skill extraction, candidate-profile classification, semantic job matching, explainable candidate ranking and recruiter review.

### Objective

Build a secure, explainable recruitment-support application that helps recruiters organize resumes and compare candidate evidence against the requirements of a specific job description.

The system should:

1. Accept resumes in supported document formats.
2. Extract structured information and job-relevant evidence.
3. Identify skills, tools, certifications, education and experience statements.
4. Normalize skill names and synonyms.
5. Compare candidate evidence with explicit job requirements using rules and semantic embeddings.
6. Produce an explainable match score and a ranked review queue.
7. Allow a recruiter to inspect evidence, correct extracted information and make the final decision.

**This is a decision-support tool, not an automated hiring decision-maker.** It must not automatically reject, shortlist or hire a person without meaningful human review. A match score is an aid to review, not a measure of a person's worth or future performance.

---

## 2. Target Users and Roles

### Recruiter
- Create and manage job descriptions.
- Upload resumes individually or in batches.
- Review extracted profiles and evidence.
- Compare candidates against a job's requirements.
- Add notes, labels and review outcomes.
- Export permitted reports.

### Hiring Manager
- View candidates shared for a job.
- Inspect match explanations and source evidence.
- Add structured feedback.
- Record a human review outcome.

### Administrator
- Manage users, roles and workspace settings.
- Configure retention and security policies.
- Manage skill taxonomy and model versions.
- View audit events and system health.
- Configure authorized model providers and embedding models.

Access must be enforced on the backend. Hiding a button in the frontend is not authorization.

---

## 3. Scope

### In scope for the first release
- Secure authentication and role-based access control.
- Job description creation and structured requirement management.
- PDF, DOCX and TXT resume upload.
- Text extraction and document validation.
- Structured resume profile extraction.
- Skill normalization and evidence linking.
- Embedding-based semantic matching.
- Configurable, explainable matching and ranking.
- Candidate review screen and manual corrections.
- Audit history, retention controls and deletion.
- CSV export of approved, job-relevant results.
- Automated tests and Docker-based local deployment.

### Out of scope for the first release
- Autonomous hiring, rejection or offer decisions.
- Facial analysis, voice analysis or personality inference.
- Emotion, attitude, culture-fit or protected-trait prediction.
- Social-media scraping or background checks.
- Automatic inference of age, gender, ethnicity, religion, disability, family status or other sensitive/protected traits.
- Candidate ranking based on names, photographs, addresses, graduation dates or other irrelevant proxy features.
- Sending candidate data to external AI services without explicit administrator configuration and appropriate disclosure.

---

## 4. Core Functional Requirements

### 4.1 Authentication and Workspace Management

- Implement secure login and logout.
- Support recruiter, hiring-manager and administrator roles.
- Associate jobs, resumes, candidates and review records with a workspace.
- Enforce workspace isolation for every API and file operation.
- Hash passwords using a modern password-hashing mechanism supported by Spring Security.
- Add rate limiting to login and upload endpoints.
- Maintain an audit trail for sensitive actions.

### 4.2 Job Description Management

Allow recruiters to create a job with:
- Job title and department.
- Job description.
- Required skills.
- Preferred skills.
- Minimum or preferred experience ranges, where genuinely job-related.
- Required certifications or qualifications, where applicable.
- Optional responsibilities and domain context.
- Requirement importance or weight.

Provide a review step after parsing a job description. The recruiter must be able to edit the extracted requirements before matching begins.

Each requirement should have:
- Canonical skill or qualification name.
- Requirement type: required, preferred or contextual.
- Weight or importance.
- Optional acceptable synonyms.
- Optional evidence guidance.

Do not silently convert vague language into rigid rejection criteria. Keep the original job-description wording available for review.

### 4.3 Resume Upload and Validation

Supported formats for the initial release:
- PDF (`.pdf`).
- Microsoft Word DOCX (`.docx`).
- Plain text (`.txt`).

Requirements:
- Validate extension, MIME type and file signature where possible.
- Set configurable file-size and page-count limits.
- Reject unsupported, encrypted or malformed documents with clear errors.
- Generate a unique internal document identifier and safe storage key.
- Store files outside the public web root or in private object storage.
- Do not trust the uploaded filename.
- Scan uploads for malware using an available scanning integration before processing in production.
- Support batch upload with per-file status and error details.
- Detect likely duplicate uploads using a cryptographic file hash, while keeping duplicate handling configurable.

Scanned PDF OCR may be an optional later capability. If enabled, display that extracted text came from OCR and may contain recognition errors.

### 4.4 Resume Text Extraction

Use Java libraries to extract text from documents. Recommended options:
- Apache Tika for document detection and text extraction.
- Apache PDFBox for PDF-specific processing when needed.
- Apache POI for DOCX-specific handling when needed.

Requirements:
- Preserve page numbers or document offsets where possible.
- Normalize whitespace without destroying meaningful section boundaries.
- Detect empty or low-quality extraction.
- Record parser name, parser version and extraction status.
- Store the original file separately from normalized extracted text.
- Never execute macros, embedded scripts or active document content.

### 4.5 Resume Profile Extraction

Extract structured, job-relevant fields:
- Skills and technologies.
- Employment history and role descriptions.
- Project descriptions.
- Education and qualifications.
- Certifications.
- Tools, platforms and domain terms.
- Evidence text and source location for each extracted item.

Each extracted item should include:
- Normalized value.
- Original text span.
- Source page or offset, where available.
- Extraction method: rule-based, model-based or manually corrected.
- Confidence indicator.
- Review status.

Important rules:
- Treat missing information as **unknown**, not proof that a candidate lacks a skill or qualification.
- Do not infer a skill only from a job title when no supporting evidence exists.
- Distinguish a skill explicitly claimed by the candidate from a skill inferred from project or work descriptions.
- Do not invent years of experience. If dates are ambiguous, mark the duration as uncertain.
- Allow recruiters to correct extracted fields and preserve the correction history.

### 4.6 Skill Normalization and Taxonomy

Create a maintainable skill taxonomy with:
- Canonical skill names.
- Synonyms and common abbreviations.
- Related but distinct skills.
- Skill categories such as programming language, framework, database, cloud platform and methodology.
- Optional version or proficiency metadata when explicitly supported by evidence.

Examples:
- `JS` → `JavaScript`.
- `Postgres` → `PostgreSQL`.
- `Spring` should not automatically be treated as every Spring sub-framework.
- `C++` must not be treated as interchangeable with `C`.

The taxonomy must be editable and versioned. Avoid fuzzy matching rules that merge materially different technologies.

### 4.7 Candidate Profile Classification

Classify profiles into configurable job-relevant categories, for example:
- Software Engineering.
- Data Engineering.
- Machine Learning / AI Engineering.
- DevOps / Platform Engineering.
- Quality Engineering.
- Cybersecurity.
- Product / Business Analysis.

Classification must be advisory. Show the main evidence and confidence. Permit multiple labels and manual correction. Do not use classification as an automatic exclusion rule.

### 4.8 Job-to-Candidate Matching

Use a hybrid matching approach:

1. **Structured matching:** compare normalized skills and explicit qualifications.
2. **Semantic matching:** compare job requirement text with relevant resume sections using embeddings.
3. **Evidence matching:** identify supporting resume passages for each matched requirement.
4. **Experience context:** summarize relevant work and project evidence without assuming that job-title similarity proves competence.

For each candidate-job pair, return:
- Overall match score.
- Required-skill coverage.
- Preferred-skill coverage.
- Semantic relevance score.
- Evidence coverage.
- Unknown or unverified requirements.
- Matched requirements and their evidence.
- Requirements with no located evidence.
- Model and taxonomy versions used.

The scoring formula must be configurable and documented. A possible initial design is:

`match_score = required_coverage_weight + preferred_coverage_weight + semantic_relevance_weight + evidence_quality_weight`

Weights must be normalized, visible to administrators and validated before use. The product must not present a score as an objective probability of job success.

Important scoring safeguards:
- Required and preferred criteria must remain distinguishable.
- Missing resume information must not be silently scored as a definite mismatch.
- Do not score protected characteristics or their proxies.
- Do not use name, photograph, gender, age, ethnicity, religion, disability, family status, nationality (unless a lawful, explicit work-authorization requirement is separately handled), address, graduation year or employment gaps as general ranking features.
- Do not infer personality, mental health, honesty, motivation or cultural fit.
- Display confidence and data completeness separately from match score.
- Allow a recruiter to inspect and challenge every material match explanation.

### 4.9 Explainable Candidate Ranking

For each job, show a review queue sorted by the configured match score, but make the ranking controls transparent.

Display:
- Candidate reference ID.
- Match score and confidence/data-completeness indicator as separate values.
- Matched required skills.
- Matched preferred skills.
- Evidence excerpts with page references where available.
- Requirements without located evidence.
- Uncertain or conflicting extracted information.
- Model version and assessment timestamp.

Recruiters must be able to sort by individual criteria, filter the queue and inspect candidates without relying only on a single composite score.

Do not automatically reject candidates below a threshold. A threshold may be used to organize a review queue only if it is clearly labeled and does not trigger an adverse decision.

### 4.10 Human Review and Feedback

Recruiters and authorized hiring managers can:
- Mark a profile as not reviewed, under review, needs clarification, reviewed or closed.
- Add job-related notes.
- Correct extracted skills and experience details.
- Record a human decision with a reason selected from configurable job-related categories.
- Request another reviewer.
- View changes and prior assessments.

Human decisions must be distinguishable from model-generated outputs. Do not automatically train on recruiter decisions without a separate, reviewed model-governance process.

### 4.11 Reporting and Export

Provide job-level reports containing:
- Number of resumes received and processed.
- Processing failures and reasons.
- Candidate match summaries.
- Skill coverage across the applicant pool.
- Frequently occurring skills.
- Review status counts.
- Assessment model and taxonomy versions.

Export CSV in the first release. Ensure exports only include fields the requesting user is authorized to access. Exclude unnecessary personal data by default.

---

## 5. AI / NLP Architecture

### 5.1 Recommended Approach

Use a hybrid Java-first architecture:
- Deterministic parsing and validation for document processing.
- Rule-based and taxonomy-based extraction for clear skill names and aliases.
- Named-entity or token-classification models for structured extraction where useful.
- Embedding models for semantic similarity.
- Explicit scoring logic in Java for transparent, testable ranking.
- LLM calls, if enabled, only for bounded extraction or explanation tasks with schema validation and evidence requirements.

Do not make an LLM the sole source of candidate scores. The ranking calculation must be reproducible from stored structured evidence and versioned configuration.

### 5.2 Java Libraries and Services

Recommended stack:
- Java 21.
- Spring Boot for REST APIs and application services.
- Spring Security for authentication and authorization.
- Apache Tika for document detection and text extraction.
- Apache PDFBox for PDF handling when needed.
- Apache POI for DOCX handling when needed.
- ONNX Runtime Java or DJL for local model inference.
- Spring AI or LangChain4j as optional abstractions for LLM and embedding integrations.
- PostgreSQL for relational data.
- pgvector or Qdrant for embedding storage and similarity search.
- Redis for caching and asynchronous job coordination, if needed.
- Maven for dependency management.
- JUnit 5, Mockito and Testcontainers for testing.
- Docker Compose for local development.

Use compatible, maintained library releases selected during implementation. Pin dependency versions and document upgrades. Avoid adding both Spring AI and LangChain4j unless there is a clear architectural need.

### 5.3 Model Strategy

- Prefer a locally hosted embedding model or a model exported to ONNX for predictable data handling.
- Keep model access behind interfaces so the model can be replaced without changing business logic.
- Store model name, version, dimensions and preprocessing configuration.
- Validate embedding dimensions and input limits.
- Cache embeddings by document-content hash and model version where appropriate.
- Do not mix vectors from incompatible model versions in the same index.
- If using a hosted provider, require administrator configuration, disclose data transfer, minimize sent content and provide a local/offline option where feasible.

### 5.4 Evaluation Strategy

Create a versioned, human-reviewed evaluation dataset containing representative job descriptions and resumes. Use synthetic or appropriately licensed/de-identified data for development.

Measure:
- Skill extraction precision, recall and F1.
- Entity/section extraction accuracy.
- Requirement-level match precision and recall.
- Ranking quality using metrics such as Precision@K, Recall@K and NDCG@K.
- Evidence attribution accuracy.
- Calibration or confidence reliability where confidence is exposed.
- Processing latency and failure rate.

Do not claim model quality based only on a few hand-picked examples. Record dataset provenance, annotation guidelines and evaluation limitations.

---

## 6. Responsible AI, Fairness and Privacy Requirements

Hiring is a high-impact use case. These safeguards are release requirements, not optional enhancements.

### 6.1 Human Oversight
- The system must not make final hiring, rejection or offer decisions.
- Every ranking must be reviewable by an authorized human.
- Show evidence and uncertainty, not just a number.
- Allow corrections and appeals within the organization's workflow.
- Record when a human overrides or disagrees with an assessment.

### 6.2 Fairness and Bias Controls
- Exclude protected characteristics and sensitive personal attributes from scoring.
- Do not infer protected traits from names, photographs, schools, locations or other proxies.
- Do not use facial, voice, emotion or personality analysis.
- Use job-related criteria approved by the hiring organization.
- Test extraction and ranking across varied resume formats, writing styles, career paths and employment histories.
- Where lawful and appropriately governed, conduct a separate fairness audit using voluntary, protected audit data stored separately from operational ranking data. Never feed that audit data into candidate scoring.
- Document limitations and remediate materially unequal error rates before deployment.

### 6.3 Privacy and Security
- Collect only information needed for the recruitment workflow.
- Encrypt data in transit and at rest.
- Store resumes in private storage, not public static directories.
- Apply least-privilege access to resumes, extracted text and exports.
- Log access to candidate records and document downloads.
- Configure retention periods per workspace and jurisdiction.
- Support deletion of a candidate's resume, extracted data, embeddings and related derived records, subject to applicable retention obligations.
- Avoid logging full resume text, credentials or sensitive personal information.
- Ensure external AI providers do not use submitted data for model training unless the organization has explicitly approved that arrangement.
- Conduct legal and privacy review for the deployment jurisdiction before using the system for real recruitment.

---

## 7. Application Architecture

### 7.1 High-Level Components

1. **Web UI:** job management, resume upload, candidate review and reports.
2. **REST API:** authentication, jobs, candidates, uploads, matching and review actions.
3. **Document Processing Service:** validation, text extraction and normalization.
4. **Profile Extraction Service:** skills, experience, education and evidence extraction.
5. **Skill Taxonomy Service:** canonicalization and synonym resolution.
6. **Embedding Service:** vector generation and semantic retrieval.
7. **Matching Engine:** requirement matching, evidence aggregation and scoring.
8. **Review and Audit Service:** human review, corrections and traceability.
9. **Persistence Layer:** PostgreSQL and private document storage.
10. **Background Processing:** asynchronous parsing and matching jobs.

### 7.2 Suggested Package Structure

```text
resume-screening-engine/
├── src/
│   ├── main/
│   │   ├── java/com/example/resumescreening/
│   │   │   ├── config/
│   │   │   ├── security/
│   │   │   ├── auth/
│   │   │   ├── workspace/
│   │   │   ├── job/
│   │   │   ├── candidate/
│   │   │   ├── resume/
│   │   │   ├── document/
│   │   │   ├── extraction/
│   │   │   ├── taxonomy/
│   │   │   ├── embedding/
│   │   │   ├── matching/
│   │   │   ├── review/
│   │   │   ├── reporting/
│   │   │   ├── audit/
│   │   │   └── common/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── db/migration/
│   │       ├── templates/
│   │       └── static/
│   └── test/
│       └── java/com/example/resumescreening/
├── docs/
├── docker/
├── scripts/
├── compose.yaml
├── Dockerfile
├── pom.xml
├── README.md
└── PRD.md
```

Adapt package names and structure to the generated project. Keep controllers thin and place business logic in services/domain components.

---

## 8. Data Model (Initial)

Create migrations and relationships for the following entities:

### User
- id
- workspace_id
- name
- email
- password_hash
- role
- status
- created_at
- updated_at

### Workspace
- id
- name
- settings_json
- retention_policy
- created_at

### Job
- id
- workspace_id
- title
- description
- department
- status
- created_by
- created_at
- updated_at

### JobRequirement
- id
- job_id
- canonical_skill_id or requirement_key
- requirement_text
- requirement_type
- weight
- is_active

### Candidate
- id
- workspace_id
- candidate_reference
- display_name and contact fields, where required for recruiter workflow
- created_at

Personal identifiers must not be used in scoring features. Keep them logically separate from the feature representation used by the matching engine.

### ResumeDocument
- id
- candidate_id
- storage_key
- original_filename
- mime_type
- file_hash
- extraction_status
- parser_version
- uploaded_at
- deleted_at

### ResumeProfile
- id
- candidate_id
- resume_document_id
- normalized_text_reference
- profile_json
- extraction_version
- review_status
- created_at

### ExtractedEvidence
- id
- resume_profile_id
- field_type
- canonical_value
- original_text
- page_number or text_offset
- extraction_method
- confidence
- review_status

### Skill
- id
- canonical_name
- category
- taxonomy_version

### SkillAlias
- id
- skill_id
- alias

### CandidateJobAssessment
- id
- candidate_id
- job_id
- score
- confidence_or_completeness
- score_breakdown_json
- matched_evidence_json
- missing_or_unknown_requirements_json
- embedding_model_version
- scoring_config_version
- created_at

### CandidateReview
- id
- candidate_id
- job_id
- reviewer_id
- review_status
- decision
- reason
- notes
- created_at
- updated_at

### AuditEvent
- id
- workspace_id
- actor_id
- action
- resource_type
- resource_id
- metadata_redacted_json
- timestamp

Add indexes for workspace ownership, job/candidate lookups, review status, file hashes and vector similarity operations. Use foreign keys and appropriate deletion rules. Avoid storing duplicate resume text in multiple tables without a clear need.

---

## 9. API Requirements

Use consistent response formats, validation errors and pagination. Version APIs under `/api/v1`.

Suggested endpoints:

### Authentication
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

### Jobs
- `POST /api/v1/jobs`
- `GET /api/v1/jobs`
- `GET /api/v1/jobs/{jobId}`
- `PUT /api/v1/jobs/{jobId}`
- `POST /api/v1/jobs/{jobId}/requirements/parse`
- `PUT /api/v1/jobs/{jobId}/requirements`

### Resumes and Candidates
- `POST /api/v1/jobs/{jobId}/resumes`
- `POST /api/v1/jobs/{jobId}/resumes/batch`
- `GET /api/v1/jobs/{jobId}/candidates`
- `GET /api/v1/candidates/{candidateId}`
- `GET /api/v1/candidates/{candidateId}/evidence`
- `DELETE /api/v1/candidates/{candidateId}/resume`

### Matching and Review
- `POST /api/v1/jobs/{jobId}/assessments`
- `GET /api/v1/jobs/{jobId}/assessments`
- `GET /api/v1/candidates/{candidateId}/assessments/{jobId}`
- `POST /api/v1/candidates/{candidateId}/reviews`
- `PUT /api/v1/candidates/{candidateId}/evidence/{evidenceId}`

### Reporting and Administration
- `GET /api/v1/jobs/{jobId}/reports/summary`
- `GET /api/v1/jobs/{jobId}/exports/candidates.csv`
- `GET /api/v1/admin/audit-events`
- `GET /api/v1/health`

Every endpoint must enforce authentication, role permissions and workspace ownership. Do not expose internal storage paths or raw stack traces.

---

## 10. User Interface Requirements

### Main Screens
1. **Login:** secure sign-in and session handling.
2. **Dashboard:** job count, resume processing status, review queue and recent activity.
3. **Jobs:** create, edit, archive and search jobs.
4. **Job Requirements:** inspect and edit parsed requirements and weights.
5. **Resume Upload:** drag-and-drop upload, format/size validation and per-file progress.
6. **Candidate Queue:** sortable and filterable review list.
7. **Candidate Profile:** extracted skills, experience, education, evidence excerpts and original resume viewer/download when authorized.
8. **Match Explanation:** criterion-level evidence, match breakdown, unknown fields and model/version information.
9. **Review Panel:** human status, notes, correction and review history.
10. **Reports:** job-level summaries and CSV export.
11. **Admin Settings:** users, roles, retention, model configuration and audit events.

### UX Requirements
- Use clear status labels: uploaded, processing, needs review, ready, failed and deleted.
- Show loading, empty and error states.
- Keep match score and confidence/data completeness visually separate.
- Never use color alone to communicate ranking or risk.
- Make evidence excerpts easy to trace to their source page.
- Explain that scores are decision-support estimates, not hiring recommendations.
- Provide accessible forms, keyboard navigation and responsive layouts.

---

## 11. Non-Functional Requirements

### Performance
- Process resumes asynchronously.
- Configure upload size, page count, worker concurrency and timeouts.
- Avoid loading large documents entirely into memory when streaming is possible.
- Cache embeddings by content hash and model version.
- Paginate candidate queues and reports.
- Record p50 and p95 processing and API latency.
- Establish performance targets after benchmarking on representative infrastructure; do not claim unmeasured throughput.

### Reliability
- Use idempotent processing jobs.
- Track each processing stage and retry only safe transient failures.
- Prevent duplicate assessments for the same input/model/configuration unless explicitly requested.
- Preserve failed-job diagnostics without logging resume contents.
- Support reprocessing when parser, taxonomy or model versions change.

### Maintainability
- Use constructor injection and clear service interfaces.
- Keep parsing, extraction, embedding, matching and presentation separate.
- Use database migrations.
- Pin dependencies and document model assets.
- Add structured logs, metrics and health checks.

### Accessibility and Compatibility
- Support current major desktop browsers.
- Use responsive recruiter workflows.
- Provide keyboard-accessible forms and tables.
- Avoid requiring a GPU for the baseline deployment; CPU inference must be supported for the initial model choice or explicitly documented otherwise.

---

## 12. Testing and Quality Assurance

### Unit Tests
- File type and size validation.
- PDF/DOCX/TXT text extraction.
- Skill alias normalization.
- Section and evidence extraction.
- Requirement weighting and score calculations.
- Unknown versus missing evidence handling.
- Workspace authorization rules.
- Deletion and retention logic.

### Integration Tests
- End-to-end resume upload and processing.
- Batch upload with mixed valid and invalid documents.
- Job requirement creation and editing.
- Embedding generation and retrieval.
- Candidate assessment creation.
- Review correction and audit history.
- CSV export authorization.
- Cross-workspace access denial.

### AI Evaluation Tests
- Use a labeled, human-reviewed evaluation set.
- Measure extraction precision, recall and F1.
- Measure requirement match and ranking metrics.
- Verify evidence excerpts genuinely support claimed skills.
- Test different resume templates, writing styles and document quality.
- Test ambiguous, contradictory and missing information.
- Compare performance across appropriately designed fairness test cases.
- Add regression tests for every material model or taxonomy update.

### Security Tests
- Unauthorized upload/download attempts.
- Cross-workspace object access.
- Malformed and oversized documents.
- Malicious filenames and document payloads.
- Rate-limit behavior.
- Secrets and personal data leakage in logs.
- Access control for exports and audit records.

---

## 13. Deployment and DevOps

Recommended initial deployment:
- Java 21 runtime.
- Spring Boot executable application.
- PostgreSQL database.
- pgvector extension or separate vector database if selected.
- Private local volume or private object storage for resumes.
- Optional Redis for background queue/cache coordination.
- Docker and Docker Compose for reproducible deployment.
- GitHub Actions for build, test and container image pipeline.

Deployment requirements:
- Separate development, test and production configuration.
- Keep secrets in environment variables or a secret manager.
- Run database migrations safely during deployment.
- Disable development/debug endpoints in production.
- Enable HTTPS and secure cookies.
- Configure backups and restore tests.
- Configure log redaction and retention.
- Document model artifact licensing and deployment requirements.

Do not deploy private resume files or database contents to GitHub Pages or a public repository. GitHub Pages is static hosting and is not appropriate for the secure backend or private candidate data. The application should be deployed to a backend-capable host or private infrastructure.

---

## 14. Implementation Phases

### Phase 1 — Foundation and Security
- Create Spring Boot project with Java 21 and Maven.
- Configure database and migrations.
- Implement authentication, roles and workspace isolation.
- Add audit logging and private document storage.
- Establish CI and automated test execution.

### Phase 2 — Job and Resume Ingestion
- Implement job creation and requirement editing.
- Implement PDF, DOCX and TXT upload.
- Add file validation and text extraction.
- Add asynchronous processing status and error handling.

### Phase 3 — Profile and Skill Extraction
- Implement section extraction and structured profile representation.
- Build the skill taxonomy and alias normalization.
- Store evidence excerpts and source locations.
- Add recruiter correction workflow.

### Phase 4 — Embeddings and Matching
- Integrate a versioned embedding model.
- Implement structured and semantic matching.
- Build explainable scoring and criterion-level evidence.
- Display confidence/data completeness separately.
- Add ranking evaluation and regression tests.

### Phase 5 — Recruiter Review and Reporting
- Build candidate queue and profile views.
- Add review notes, statuses and human decisions.
- Implement audit history and CSV exports.
- Add job-level analytics.

### Phase 6 — Hardening and Release
- Complete security and privacy testing.
- Run extraction, ranking and fairness evaluations.
- Tune processing and inference performance.
- Configure Docker deployment, backup and monitoring.
- Complete documentation and administrator guide.

Do not start the next phase by assuming the previous phase is complete. Verify its acceptance criteria and tests first.

---

## 15. Acceptance Criteria

The first release is ready for controlled evaluation when:

- [x] Users can sign in and access only authorized workspaces.
- [x] Recruiters can create and edit jobs and requirements.
- [x] PDF, DOCX and TXT resumes are validated and processed.
- [x] Invalid documents fail safely with useful error messages.
- [x] Profiles contain extracted information linked to source evidence.
- [x] Skill aliases normalize correctly without merging distinct skills.
- [x] Missing information is represented as unknown rather than a definite negative.
- [x] Matching combines structured criteria and semantic relevance.
- [x] Every material score component is explainable and versioned.
- [x] Recruiters can correct extraction results and review candidates.
- [x] No automated rejection or hiring decision is performed.
- [x] Sensitive/protected traits and prohibited proxies are excluded from scoring.
- [x] Candidate files and derived data can be deleted according to retention policy.
- [x] Audit events record access and important changes without exposing resume content.
- [x] Tests cover extraction, scoring, authorization and failure paths.
- [x] Ranking evaluation and fairness limitations are documented.
- [x] The application can be deployed using the documented Docker workflow.

---

## 16. Instructions for Antigravity Studio

Use this PRD as the implementation specification. Follow these rules:

1. Inspect the complete repository and existing dependencies before changing files.
2. Implement the application primarily in Java 21 using Spring Boot.
3. Do not replace Java with Python for core backend, extraction orchestration, matching, scoring or workflow logic.
4. Python may be used only as an optional offline model-training/evaluation tool if explicitly needed; production application services must remain Java-first.
5. Keep model inference behind Java interfaces so local ONNX/DJL models or approved remote providers can be swapped.
6. Implement the matching calculation in deterministic, testable Java code.
7. Never use protected characteristics or proxy features to score candidates.
8. Never implement automatic hiring, rejection or offer decisions.
9. Require source evidence for extracted skills and explanations.
10. Do not fabricate candidate data, match scores, certifications or experience.
11. Do not use real personal resumes in test fixtures. Use synthetic or properly de-identified examples.
12. Do not expose uploaded documents publicly or commit them to Git.
13. Add tests with each feature and run the full test suite before marking a phase complete.
14. Keep secrets, API keys and database credentials out of source control.
15. Document setup, configuration, model assets, data retention and limitations.
16. Do not mark incomplete or untested features as complete.

### Final Product Goal

Deliver a Java-first, secure and explainable resume intelligence platform that extracts job-relevant evidence, helps recruiters compare resumes with explicit job requirements, and supports consistent human review. Prioritize evidence quality, privacy, fairness, transparency and reliability over aggressive automation or feature count.
