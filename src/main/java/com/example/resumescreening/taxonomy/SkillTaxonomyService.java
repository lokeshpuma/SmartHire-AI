package com.example.resumescreening.taxonomy;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SkillTaxonomyService {

    private static final Logger log = LoggerFactory.getLogger(SkillTaxonomyService.class);

    private final SkillRepository skillRepository;
    private final SkillAliasRepository skillAliasRepository;
    private final String currentTaxonomyVersion;

    // Cache alias/name (lowercase) -> Canonical Skill
    private final Map<String, Skill> aliasToSkillCache = new ConcurrentHashMap<>();
    private final List<Skill> allSkillsCache = new ArrayList<>();

    public SkillTaxonomyService(SkillRepository skillRepository,
                                SkillAliasRepository skillAliasRepository,
                                @Value("${app.taxonomy.version:1.0.0}") String currentTaxonomyVersion) {
        this.skillRepository = skillRepository;
        this.skillAliasRepository = skillAliasRepository;
        this.currentTaxonomyVersion = currentTaxonomyVersion;
    }

    @PostConstruct
    @Transactional
    public void initTaxonomy() {
        if (skillRepository.count() == 0) {
            log.info("Bootstrapping default skill taxonomy version {}", currentTaxonomyVersion);
            seedDefaultSkills();
        }
        refreshCache();
    }

    @Transactional(readOnly = true)
    public void refreshCache() {
        aliasToSkillCache.clear();
        allSkillsCache.clear();
        List<Skill> skills = skillRepository.findAllWithAliases();
        allSkillsCache.addAll(skills);

        for (Skill s : skills) {
            aliasToSkillCache.put(s.getCanonicalName().toLowerCase(), s);
            if (s.getAliases() != null) {
                for (SkillAlias a : s.getAliases()) {
                    aliasToSkillCache.put(a.getAlias().toLowerCase(), s);
                }
            }
        }
        log.info("Loaded {} skills with {} alias mappings into taxonomy cache", skills.size(), aliasToSkillCache.size());
    }

    /**
     * Resolves an input term to its canonical skill name.
     * Prevents false merging such as C++ vs C, and handles exact synonyms (JS -> JavaScript).
     */
    public Optional<Skill> resolveSkill(String term) {
        if (term == null || term.isBlank()) return Optional.empty();
        String normalized = term.trim().toLowerCase();
        return Optional.ofNullable(aliasToSkillCache.get(normalized));
    }

    /**
     * Extracts all known canonical skills present in a text snippet.
     */
    public List<SkillMatch> extractSkillsFromText(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        Map<String, SkillMatch> matched = new LinkedHashMap<>();

        // Sort skills by alias length descending so longer matches take precedence (e.g. "C++" before "C", "Spring Boot" before "Spring")
        List<Map.Entry<String, Skill>> sortedEntries = new ArrayList<>(aliasToSkillCache.entrySet());
        sortedEntries.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

        for (Map.Entry<String, Skill> entry : sortedEntries) {
            String alias = entry.getKey();
            Skill skill = entry.getValue();

            // Word-boundary pattern taking special characters (like C++, C#, .NET) into account
            String regex = buildBoundaryRegex(alias);
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                if (!matched.containsKey(skill.getCanonicalName())) {
                    matched.put(skill.getCanonicalName(), new SkillMatch(
                            skill,
                            matcher.group(),
                            matcher.start(),
                            matcher.end()
                    ));
                }
            }
        }

        return new ArrayList<>(matched.values());
    }

    private String buildBoundaryRegex(String term) {
        // Special case for terms ending or starting with symbols like C++, C#, .NET
        if (term.equalsIgnoreCase("c++")) {
            return "(?i)(?<=^|[^a-zA-Z0-9])c\\+\\+(?=[^a-zA-Z0-9]|$)";
        }
        if (term.equalsIgnoreCase("c#")) {
            return "(?i)(?<=^|[^a-zA-Z0-9])c#(?=[^a-zA-Z0-9]|$)";
        }
        if (term.equalsIgnoreCase("c")) {
            // Strict match for C language: isolated C, not followed by ++ or #
            return "(?i)(?<=^|[^a-zA-Z0-9])c(?=[^a-zA-Z0-9+#]|$)";
        }
        if (term.equalsIgnoreCase(".net")) {
            return "(?i)(?<=^|[^a-zA-Z0-9])\\.net(?=[^a-zA-Z0-9]|$)";
        }
        // General word boundary
        return "(?i)\\b" + Pattern.quote(term) + "\\b";
    }

    @Transactional
    public Skill addCustomSkill(String canonicalName, SkillCategory category, String description, List<String> aliases) {
        Skill skill = new Skill(canonicalName, category, description);
        skill.setTaxonomyVersion(currentTaxonomyVersion);
        if (aliases != null) {
            for (String a : aliases) {
                skill.addAlias(a);
            }
        }
        Skill saved = skillRepository.save(skill);
        refreshCache();
        return saved;
    }

    public List<Skill> getAllSkills() {
        return Collections.unmodifiableList(allSkillsCache);
    }

    public String getCurrentTaxonomyVersion() {
        return currentTaxonomyVersion;
    }

    private void seedDefaultSkills() {
        // Languages
        addSkill("Java", SkillCategory.PROGRAMMING_LANGUAGE, "Java platform", "java 21", "java 17", "java 11", "java 8", "core java");
        addSkill("Python", SkillCategory.PROGRAMMING_LANGUAGE, "Python language", "py", "python3", "python 3");
        addSkill("C++", SkillCategory.PROGRAMMING_LANGUAGE, "C++ programming language", "cpp", "cplusplus");
        addSkill("C", SkillCategory.PROGRAMMING_LANGUAGE, "C programming language");
        addSkill("C#", SkillCategory.PROGRAMMING_LANGUAGE, "C# language", "csharp", "c sharp");
        addSkill("JavaScript", SkillCategory.PROGRAMMING_LANGUAGE, "JavaScript language", "js", "ecmascript");
        addSkill("TypeScript", SkillCategory.PROGRAMMING_LANGUAGE, "TypeScript language", "ts");
        addSkill("Go", SkillCategory.PROGRAMMING_LANGUAGE, "Golang", "golang");
        addSkill("Rust", SkillCategory.PROGRAMMING_LANGUAGE, "Rust language");
        addSkill("Kotlin", SkillCategory.PROGRAMMING_LANGUAGE, "Kotlin language");
        addSkill("Swift", SkillCategory.PROGRAMMING_LANGUAGE, "Swift language");
        addSkill("SQL", SkillCategory.PROGRAMMING_LANGUAGE, "Structured Query Language");
        addSkill("HTML", SkillCategory.PROGRAMMING_LANGUAGE, "Hypertext Markup Language", "html5");
        addSkill("CSS", SkillCategory.PROGRAMMING_LANGUAGE, "Cascading Style Sheets", "css3");

        // Frameworks
        addSkill("Spring Boot", SkillCategory.FRAMEWORK, "Spring Boot Java framework", "springboot", "spring-boot");
        addSkill("Spring", SkillCategory.FRAMEWORK, "Spring Framework Core", "spring framework");
        addSkill("React", SkillCategory.FRAMEWORK, "React JavaScript Library", "reactjs", "react.js");
        addSkill("Angular", SkillCategory.FRAMEWORK, "Angular web framework", "angularjs", "angular 2+");
        addSkill("Vue.js", SkillCategory.FRAMEWORK, "Vue JavaScript framework", "vue", "vuejs");
        addSkill("Node.js", SkillCategory.FRAMEWORK, "Node.js JavaScript runtime", "nodejs", "node");
        addSkill("Express.js", SkillCategory.FRAMEWORK, "Express web framework", "express", "expressjs");
        addSkill("Django", SkillCategory.FRAMEWORK, "Django Python framework");
        addSkill("FastAPI", SkillCategory.FRAMEWORK, "FastAPI Python framework");
        addSkill("Flask", SkillCategory.FRAMEWORK, "Flask Python framework");
        addSkill(".NET", SkillCategory.FRAMEWORK, ".NET Framework", "dotnet", ".net core");
        addSkill("Next.js", SkillCategory.FRAMEWORK, "Next.js React framework", "nextjs");

        // Databases
        addSkill("PostgreSQL", SkillCategory.DATABASE, "PostgreSQL relational database", "postgres", "psql");
        addSkill("MySQL", SkillCategory.DATABASE, "MySQL database");
        addSkill("MongoDB", SkillCategory.DATABASE, "MongoDB document database", "mongo");
        addSkill("Redis", SkillCategory.DATABASE, "Redis in-memory store");
        addSkill("Elasticsearch", SkillCategory.DATABASE, "Elasticsearch distributed search", "elastic");
        addSkill("Cassandra", SkillCategory.DATABASE, "Apache Cassandra NoSQL");
        addSkill("DynamoDB", SkillCategory.DATABASE, "Amazon DynamoDB", "dynamo");

        // Cloud & DevOps
        addSkill("AWS", SkillCategory.CLOUD_PLATFORM, "Amazon Web Services", "amazon web services");
        addSkill("GCP", SkillCategory.CLOUD_PLATFORM, "Google Cloud Platform", "google cloud");
        addSkill("Azure", SkillCategory.CLOUD_PLATFORM, "Microsoft Azure");
        addSkill("Docker", SkillCategory.TOOL, "Docker container platform");
        addSkill("Kubernetes", SkillCategory.TOOL, "Kubernetes container orchestrator", "k8s", "kube");
        addSkill("Terraform", SkillCategory.TOOL, "Terraform IaC");
        addSkill("Ansible", SkillCategory.TOOL, "Ansible configuration management");
        addSkill("CI/CD", SkillCategory.METHODOLOGY, "Continuous Integration and Deployment", "cicd", "continuous integration");
        addSkill("Git", SkillCategory.TOOL, "Git version control", "github", "gitlab");
        addSkill("Linux", SkillCategory.TOOL, "Linux OS", "unix", "ubuntu");

        // Architecture & Messaging
        addSkill("Apache Kafka", SkillCategory.TOOL, "Apache Kafka event streaming", "kafka");
        addSkill("RabbitMQ", SkillCategory.TOOL, "RabbitMQ message broker");
        addSkill("GraphQL", SkillCategory.ARCHITECTURE, "GraphQL query language");
        addSkill("REST API", SkillCategory.ARCHITECTURE, "RESTful Web Services", "restful", "rest apis");
        addSkill("Microservices", SkillCategory.ARCHITECTURE, "Microservices architecture", "microservice");

        // Testing & Quality
        addSkill("JUnit", SkillCategory.TESTING, "JUnit testing framework", "junit 5", "junit 4");
        addSkill("Mockito", SkillCategory.TESTING, "Mockito mocking framework");
        addSkill("Selenium", SkillCategory.TESTING, "Selenium automation tool");
        addSkill("PyTest", SkillCategory.TESTING, "PyTest Python test framework");

        // AI / ML / Data
        addSkill("Machine Learning", SkillCategory.DOMAIN, "Machine Learning", "ml");
        addSkill("Deep Learning", SkillCategory.DOMAIN, "Deep Learning", "dl");
        addSkill("Natural Language Processing", SkillCategory.DOMAIN, "NLP", "nlp");
        addSkill("Computer Vision", SkillCategory.DOMAIN, "Computer vision", "cv");
        addSkill("PyTorch", SkillCategory.FRAMEWORK, "PyTorch deep learning");
        addSkill("TensorFlow", SkillCategory.FRAMEWORK, "TensorFlow framework", "tf");
        addSkill("Scikit-Learn", SkillCategory.FRAMEWORK, "Scikit-Learn ML", "sklearn");
        addSkill("Pandas", SkillCategory.TOOL, "Pandas data analysis");
        addSkill("NumPy", SkillCategory.TOOL, "NumPy scientific computing");
        addSkill("Apache Spark", SkillCategory.TOOL, "Apache Spark distributed processing", "spark");
    }

    private void addSkill(String name, SkillCategory category, String description, String... aliases) {
        Skill skill = new Skill(name, category, description);
        skill.setTaxonomyVersion(currentTaxonomyVersion);
        for (String a : aliases) {
            skill.addAlias(a);
        }
        skillRepository.save(skill);
    }

    public static class SkillMatch {
        private final Skill skill;
        private final String matchedText;
        private final int startOffset;
        private final int endOffset;

        public SkillMatch(Skill skill, String matchedText, int startOffset, int endOffset) {
            this.skill = skill;
            this.matchedText = matchedText;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        public Skill getSkill() { return skill; }
        public String getMatchedText() { return matchedText; }
        public int getStartOffset() { return startOffset; }
        public int getEndOffset() { return endOffset; }
    }
}
