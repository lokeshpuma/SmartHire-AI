package com.example.resumescreening.config;

import com.example.resumescreening.auth.User;
import com.example.resumescreening.auth.UserRepository;
import com.example.resumescreening.job.Job;
import com.example.resumescreening.job.JobRepository;
import com.example.resumescreening.job.JobRequirement;
import com.example.resumescreening.job.RequirementType;
import com.example.resumescreening.security.UserRole;
import com.example.resumescreening.workspace.Workspace;
import com.example.resumescreening.workspace.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(WorkspaceRepository workspaceRepository,
                           UserRepository userRepository,
                           JobRepository jobRepository,
                           PasswordEncoder passwordEncoder) {
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Workspace workspace;
        if (workspaceRepository.count() == 0) {
            log.info("Bootstrapping initial workspace and default users...");
            workspace = new Workspace("Acme Tech Workspace");
            workspace.setRetentionPolicy("standard-365-days");
            workspace.setSettingsJson("{\"allowBatchUpload\":true,\"ocrEnabled\":false,\"maxFileSizeMB\":15}");
            workspace = workspaceRepository.save(workspace);

            User admin = new User(workspace, "System Administrator", "admin@example.com", passwordEncoder.encode("admin123"), UserRole.ADMINISTRATOR);
            userRepository.save(admin);

            User recruiter = new User(workspace, "Sarah Recruiter", "recruiter@example.com", passwordEncoder.encode("password123"), UserRole.RECRUITER);
            userRepository.save(recruiter);

            User hiringManager = new User(workspace, "Alex HiringManager", "manager@example.com", passwordEncoder.encode("password123"), UserRole.HIRING_MANAGER);
            userRepository.save(hiringManager);
        } else {
            workspace = workspaceRepository.findAll().get(0);
        }

        // Ensure at least 10 comprehensive jobs are available
        if (jobRepository.count() < 10) {
            log.info("Seeding at least 10 realistic job positions with structured requirements...");
            seedTenJobRequirements(workspace);
        }
    }

    private void seedTenJobRequirements(Workspace workspace) {
        UUID creatorId = userRepository.findByEmail("recruiter@example.com").map(User::getId).orElse(null);

        // Job 1: Senior Backend Java Engineer
        if (jobRepository.findByWorkspaceId(workspace.getId()).stream().noneMatch(j -> j.getTitle().equals("Senior Backend Java Engineer"))) {
            Job j1 = new Job(workspace, "Senior Backend Java Engineer", "Engineering",
                    "We are seeking an experienced Senior Backend Java Engineer to design high-throughput microservices.\n" +
                            "Requirements:\n" +
                            "- 5+ years experience with Java 17/21 and Spring Boot.\n" +
                            "- Deep expertise with PostgreSQL database optimization and schema design.\n" +
                            "- Production experience with Docker and Kubernetes.\n" +
                            "- Event-driven architecture with Apache Kafka.\n" +
                            "Preferred / Nice to have:\n" +
                            "- AWS cloud platform services.\n" +
                            "- Distributed caching with Redis.\n" +
                            "- Automated testing with JUnit and Mockito.", creatorId);
            j1.addRequirement(new JobRequirement(j1, "Java", "Proficiency in modern Java (Java 17/21)", RequirementType.REQUIRED, 1.0));
            j1.addRequirement(new JobRequirement(j1, "Spring Boot", "Building scalable microservices with Spring Boot", RequirementType.REQUIRED, 1.0));
            j1.addRequirement(new JobRequirement(j1, "PostgreSQL", "Database design and optimization with PostgreSQL", RequirementType.REQUIRED, 0.9));
            j1.addRequirement(new JobRequirement(j1, "Docker", "Containerization with Docker", RequirementType.REQUIRED, 0.8));
            j1.addRequirement(new JobRequirement(j1, "Kubernetes", "Container orchestration with Kubernetes", RequirementType.REQUIRED, 0.8));
            j1.addRequirement(new JobRequirement(j1, "Apache Kafka", "Event streaming and messaging with Kafka", RequirementType.REQUIRED, 0.8));
            j1.addRequirement(new JobRequirement(j1, "AWS", "Cloud platform deployment in AWS", RequirementType.PREFERRED, 0.6));
            j1.addRequirement(new JobRequirement(j1, "Redis", "Distributed caching with Redis", RequirementType.PREFERRED, 0.5));
            j1.addRequirement(new JobRequirement(j1, "JUnit", "Automated testing with JUnit and Mockito", RequirementType.PREFERRED, 0.5));
            jobRepository.save(j1);
        }

        // Job 2: Full Stack Software Engineer
        createJobIfMissing(workspace, creatorId,
                "Full Stack Software Engineer",
                "Product Engineering",
                "Looking for a Full Stack Software Engineer to build end-to-end features across modern web applications and cloud APIs.\n" +
                        "Requirements:\n" +
                        "- Strong experience with React, TypeScript, and JavaScript.\n" +
                        "- Backend development with Node.js and Express or REST APIs.\n" +
                        "- Relational database experience with PostgreSQL or MySQL.\n" +
                        "Preferred:\n" +
                        "- Next.js server-side rendering.\n" +
                        "- Container deployment using Docker.\n" +
                        "- API querying with GraphQL.",
                List.of(
                        new ReqDef("TypeScript", "Strong TypeScript frontend development", RequirementType.REQUIRED, 1.0),
                        new ReqDef("React", "Building responsive UIs with React", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Node.js", "Backend service development with Node.js", RequirementType.REQUIRED, 0.9),
                        new ReqDef("JavaScript", "Modern ECMAScript / JavaScript mastery", RequirementType.REQUIRED, 0.9),
                        new ReqDef("PostgreSQL", "Relational database integration", RequirementType.REQUIRED, 0.8),
                        new ReqDef("REST API", "RESTful Web API architecture and consumption", RequirementType.REQUIRED, 0.8),
                        new ReqDef("Next.js", "Server-rendered React applications with Next.js", RequirementType.PREFERRED, 0.7),
                        new ReqDef("Docker", "Containerizing full stack applications", RequirementType.PREFERRED, 0.6),
                        new ReqDef("GraphQL", "GraphQL schema and query design", RequirementType.PREFERRED, 0.5)
                )
        );

        // Job 3: Lead DevOps & Platform Engineer
        createJobIfMissing(workspace, creatorId,
                "Lead DevOps & Platform Engineer",
                "Cloud Operations",
                "Join our platform infrastructure team to manage multi-cluster Kubernetes deployments, infrastructure as code, and CI/CD pipelines.\n" +
                        "Requirements:\n" +
                        "- Hands-on Kubernetes and Docker cluster orchestration.\n" +
                        "- Infrastructure as Code with Terraform and Ansible.\n" +
                        "- Public cloud architecture on AWS.\n" +
                        "- Linux systems administration and shell scripting.\n" +
                        "Preferred:\n" +
                        "- Automation scripting with Python or Go.\n" +
                        "- CI/CD pipeline automation (GitHub Actions / GitLab CI).\n" +
                        "- Multi-cloud familiarity with GCP.",
                List.of(
                        new ReqDef("Kubernetes", "Production Kubernetes cluster management", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Docker", "Container image building and runtime management", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Terraform", "Infrastructure as Code with Terraform", RequirementType.REQUIRED, 0.9),
                        new ReqDef("AWS", "AWS cloud networking, IAM, VPC, and compute", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Linux", "Advanced Linux administration and troubleshooting", RequirementType.REQUIRED, 0.8),
                        new ReqDef("CI/CD", "Continuous Integration and Continuous Deployment pipelines", RequirementType.REQUIRED, 0.8),
                        new ReqDef("Python", "Infrastructure automation scripting with Python", RequirementType.PREFERRED, 0.6),
                        new ReqDef("Ansible", "Configuration management with Ansible", RequirementType.PREFERRED, 0.6),
                        new ReqDef("GCP", "Google Cloud Platform deployment experience", RequirementType.PREFERRED, 0.5)
                )
        );

        // Job 4: Machine Learning & AI Engineer
        createJobIfMissing(workspace, creatorId,
                "Machine Learning & AI Engineer",
                "Data & AI",
                "We are hiring a Machine Learning Engineer to develop, train, and deploy production NLP, computer vision, and predictive AI models.\n" +
                        "Requirements:\n" +
                        "- Strong Python programming and ML algorithmic foundations.\n" +
                        "- Deep Learning with PyTorch or TensorFlow.\n" +
                        "- Experience in Natural Language Processing (NLP) or Computer Vision.\n" +
                        "- Data modeling with Scikit-Learn, Pandas, and NumPy.\n" +
                        "Preferred:\n" +
                        "- Model serving using FastAPI or Flask.\n" +
                        "- Containerized model deployment with Docker.\n" +
                        "- Distributed data processing with Apache Spark.",
                List.of(
                        new ReqDef("Python", "Proficiency in Python for machine learning workflows", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Machine Learning", "Core machine learning modeling and evaluation", RequirementType.REQUIRED, 1.0),
                        new ReqDef("PyTorch", "Deep neural network development with PyTorch", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Deep Learning", "Deep learning architectures and transformer models", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Natural Language Processing", "NLP text preprocessing, embeddings, and inference", RequirementType.REQUIRED, 0.8),
                        new ReqDef("Scikit-Learn", "Feature engineering and classical ML with Scikit-Learn", RequirementType.REQUIRED, 0.8),
                        new ReqDef("Pandas", "Data manipulation and ETL with Pandas", RequirementType.REQUIRED, 0.8),
                        new ReqDef("FastAPI", "High-performance model serving APIs with FastAPI", RequirementType.PREFERRED, 0.6),
                        new ReqDef("Docker", "Packaging ML models in Docker containers", RequirementType.PREFERRED, 0.6),
                        new ReqDef("Apache Spark", "Distributed data processing with Spark", RequirementType.PREFERRED, 0.5)
                )
        );

        // Job 5: Senior Data Engineer
        createJobIfMissing(workspace, creatorId,
                "Senior Data Engineer",
                "Data Platform",
                "Seeking an experienced Senior Data Engineer to architect scalable streaming pipelines, data warehouses, and analytics stores.\n" +
                        "Requirements:\n" +
                        "- Advanced SQL and database schema modeling.\n" +
                        "- Data pipeline engineering with Python and Apache Spark.\n" +
                        "- Real-time event streaming with Apache Kafka.\n" +
                        "- Relational and NoSQL storage (PostgreSQL, MongoDB).\n" +
                        "Preferred:\n" +
                        "- AWS cloud data services.\n" +
                        "- Distributed in-memory caching with Redis.\n" +
                        "- Docker containerization.",
                List.of(
                        new ReqDef("SQL", "Complex SQL query optimization and schema design", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Python", "Data pipeline scripting with Python", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Apache Spark", "Large-scale batch and streaming data processing with Spark", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Apache Kafka", "Real-time event streaming architectures with Kafka", RequirementType.REQUIRED, 0.9),
                        new ReqDef("PostgreSQL", "Data storage, indexing, and partitioning in PostgreSQL", RequirementType.REQUIRED, 0.8),
                        new ReqDef("AWS", "AWS data ecosystem and object storage", RequirementType.PREFERRED, 0.7),
                        new ReqDef("MongoDB", "Document-oriented NoSQL storage with MongoDB", RequirementType.PREFERRED, 0.6),
                        new ReqDef("Redis", "Low-latency caching with Redis", RequirementType.PREFERRED, 0.5),
                        new ReqDef("Docker", "Containerized data services with Docker", RequirementType.PREFERRED, 0.5)
                )
        );

        // Job 6: Senior Frontend React / Next.js Engineer
        createJobIfMissing(workspace, creatorId,
                "Senior Frontend React / Next.js Engineer",
                "Web Experience",
                "We are looking for a Senior Frontend Engineer passionate about crafting accessible, high-performance web applications.\n" +
                        "Requirements:\n" +
                        "- Mastery of React, TypeScript, and modern JavaScript.\n" +
                        "- Strong semantic HTML, CSS, and modern responsive layouts.\n" +
                        "- Production experience with Next.js.\n" +
                        "Preferred:\n" +
                        "- REST API integration and state management.\n" +
                        "- GraphQL data fetching.\n" +
                        "- Automated UI and component testing.",
                List.of(
                        new ReqDef("React", "Advanced React component design, hooks, and lifecycle", RequirementType.REQUIRED, 1.0),
                        new ReqDef("TypeScript", "Strict type-safe frontend development with TypeScript", RequirementType.REQUIRED, 1.0),
                        new ReqDef("JavaScript", "Modern ECMAScript standards and async workflows", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Next.js", "Server-side rendering, routing, and optimization with Next.js", RequirementType.REQUIRED, 0.9),
                        new ReqDef("HTML", "Semantic HTML5, DOM manipulation, and web accessibility", RequirementType.REQUIRED, 0.8),
                        new ReqDef("CSS", "Modern CSS3, flexbox, grid, and CSS-in-JS/Tailwind", RequirementType.REQUIRED, 0.8),
                        new ReqDef("REST API", "Consuming and caching backend REST APIs", RequirementType.PREFERRED, 0.6),
                        new ReqDef("GraphQL", "GraphQL queries and Apollo client integration", RequirementType.PREFERRED, 0.5)
                )
        );

        // Job 7: Site Reliability Engineer (SRE)
        createJobIfMissing(workspace, creatorId,
                "Site Reliability Engineer (SRE)",
                "Infrastructure",
                "Ensure maximum uptime, resilience, and observability across our global cloud services.\n" +
                        "Requirements:\n" +
                        "- Deep Linux kernel, networking, and systems engineering.\n" +
                        "- Production container management with Docker and Kubernetes.\n" +
                        "- Scripting and automation in Python or Go.\n" +
                        "- CI/CD release management and observability.\n" +
                        "Preferred:\n" +
                        "- Infrastructure as Code with Terraform.\n" +
                        "- AWS cloud infrastructure management.\n" +
                        "- Distributed log analysis with Elasticsearch.",
                List.of(
                        new ReqDef("Linux", "Expert-level Linux administration and systems diagnostics", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Kubernetes", "Operating resilient Kubernetes production clusters", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Docker", "Container runtime security and optimization", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Python", "Reliability automation and telemetry scripting with Python", RequirementType.REQUIRED, 0.8),
                        new ReqDef("CI/CD", "Automated deployment verification and canary releases", RequirementType.REQUIRED, 0.8),
                        new ReqDef("Terraform", "Cloud provisioning with Terraform", RequirementType.PREFERRED, 0.7),
                        new ReqDef("AWS", "AWS cloud resilience and load balancing", RequirementType.PREFERRED, 0.7),
                        new ReqDef("Elasticsearch", "Centralized logging and metrics with Elasticsearch", RequirementType.PREFERRED, 0.5)
                )
        );

        // Job 8: Application Security & Cybersecurity Engineer
        createJobIfMissing(workspace, creatorId,
                "Application Security & Cybersecurity Engineer",
                "Information Security",
                "Protect our software engineering supply chain, APIs, and cloud infrastructure from emerging threats and vulnerabilities.\n" +
                        "Requirements:\n" +
                        "- Security analysis of REST APIs and web services.\n" +
                        "- Linux security hardening and access controls.\n" +
                        "- Secure containerization with Docker and Kubernetes.\n" +
                        "- Python or scripting for security test automation.\n" +
                        "Preferred:\n" +
                        "- DevSecOps pipeline integration (CI/CD security scanning).\n" +
                        "- Cloud security posture on AWS.\n" +
                        "- Secure coding practices in Java or Go.",
                List.of(
                        new ReqDef("Linux", "Operating system hardening, permissions, and audit logs", RequirementType.REQUIRED, 1.0),
                        new ReqDef("REST API", "API security, authentication tokens, and vulnerability detection", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Python", "Security tooling and vulnerability scanner scripting", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Docker", "Container vulnerability scanning and least-privilege runtimes", RequirementType.REQUIRED, 0.8),
                        new ReqDef("Kubernetes", "Kubernetes RBAC, network policies, and pod security", RequirementType.REQUIRED, 0.8),
                        new ReqDef("CI/CD", "Static (SAST) and dynamic (DAST) analysis in CI/CD", RequirementType.PREFERRED, 0.7),
                        new ReqDef("AWS", "AWS cloud security, IAM, KMS, and CloudTrail", RequirementType.PREFERRED, 0.6),
                        new ReqDef("Java", "Code audit and secure coding in Java applications", RequirementType.PREFERRED, 0.5)
                )
        );

        // Job 9: QA Automation & Test Systems Engineer
        createJobIfMissing(workspace, creatorId,
                "QA Automation & Test Systems Engineer",
                "Quality Assurance",
                "Build end-to-end automated testing frameworks for backend microservices, web apps, and data pipelines.\n" +
                        "Requirements:\n" +
                        "- Test automation in Java or Python.\n" +
                        "- Browser test automation with Selenium.\n" +
                        "- Unit and integration testing with JUnit and Mockito.\n" +
                        "- Testing REST APIs and backend endpoints.\n" +
                        "Preferred:\n" +
                        "- Integration with CI/CD build pipelines.\n" +
                        "- Containerized test runners with Docker.\n" +
                        "- Python test frameworks like PyTest.",
                List.of(
                        new ReqDef("Java", "Test automation framework engineering with Java", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Selenium", "Automated browser and UI testing using Selenium", RequirementType.REQUIRED, 1.0),
                        new ReqDef("JUnit", "Unit and regression testing suites with JUnit", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Mockito", "Mocking and stubbing service dependencies with Mockito", RequirementType.REQUIRED, 0.8),
                        new ReqDef("REST API", "Automated integration testing for RESTful APIs", RequirementType.REQUIRED, 0.8),
                        new ReqDef("CI/CD", "Automated test execution in CI/CD pipelines", RequirementType.REQUIRED, 0.7),
                        new ReqDef("Docker", "Running isolated test environments in Docker", RequirementType.PREFERRED, 0.6),
                        new ReqDef("PyTest", "API test scripting with Python and PyTest", RequirementType.PREFERRED, 0.5)
                )
        );

        // Job 10: Principal Cloud Architect
        createJobIfMissing(workspace, creatorId,
                "Principal Cloud Architect",
                "Architecture & Strategy",
                "Provide technical leadership, architectural governance, and system design for enterprise-scale distributed platforms.\n" +
                        "Requirements:\n" +
                        "- Enterprise cloud architecture on AWS.\n" +
                        "- Microservices architecture and high-throughput distributed systems.\n" +
                        "- Large-scale container platforms using Kubernetes and Docker.\n" +
                        "- Enterprise Infrastructure as Code with Terraform.\n" +
                        "Preferred:\n" +
                        "- Event streaming platforms with Apache Kafka.\n" +
                        "- Relational and multi-model database design (PostgreSQL).\n" +
                        "- Multi-cloud strategy including Google Cloud Platform (GCP).",
                List.of(
                        new ReqDef("AWS", "Enterprise cloud architecture, landing zones, and VPC design", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Microservices", "Domain-Driven Design and microservices architecture", RequirementType.REQUIRED, 1.0),
                        new ReqDef("Kubernetes", "Enterprise Kubernetes architecture and multi-tenant clusters", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Docker", "Enterprise container strategy and runtime standards", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Terraform", "Modular, multi-account Infrastructure as Code with Terraform", RequirementType.REQUIRED, 0.9),
                        new ReqDef("Apache Kafka", "High-throughput asynchronous event-driven design with Kafka", RequirementType.PREFERRED, 0.7),
                        new ReqDef("PostgreSQL", "High-availability relational database clustering", RequirementType.PREFERRED, 0.6),
                        new ReqDef("GCP", "Multi-cloud interconnect and GCP architecture", RequirementType.PREFERRED, 0.6)
                )
        );
    }

    private void createJobIfMissing(Workspace workspace, UUID creatorId, String title, String department, String description, List<ReqDef> reqDefs) {
        boolean exists = jobRepository.findByWorkspaceId(workspace.getId()).stream().anyMatch(j -> j.getTitle().equalsIgnoreCase(title));
        if (exists) return;

        Job job = new Job(workspace, title, department, description, creatorId);
        for (ReqDef rd : reqDefs) {
            JobRequirement req = new JobRequirement(job, rd.canonicalSkill, rd.text, rd.type, rd.weight);
            req.setEvidenceGuidance("Look for direct experience, achievements, or project evidence regarding " + rd.canonicalSkill);
            job.addRequirement(req);
        }
        jobRepository.save(job);
        log.info("Seeded job position: {} ({}) with {} requirements", title, department, reqDefs.size());
    }

    private static class ReqDef {
        final String canonicalSkill;
        final String text;
        final RequirementType type;
        final double weight;

        ReqDef(String canonicalSkill, String text, RequirementType type, double weight) {
            this.canonicalSkill = canonicalSkill;
            this.text = text;
            this.type = type;
            this.weight = weight;
        }
    }
}
