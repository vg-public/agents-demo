# Agentic Development Approach — Java Spring Boot API

> **14 specialized AI agents** orchestrated to handle the full SDLC for Java Spring Boot REST APIs with Oracle Database.

---

## Slide 1: Architecture Overview — Agent Ecosystem

```mermaid
flowchart TB
    classDef orchestrator fill:#FF6B35,stroke:#CC4400,stroke-width:5px,color:#fff,font-weight:bold,font-size:20px
    classDef dev fill:#4169E1,stroke:#1E3A8A,stroke-width:2px,color:#fff,font-size:20px
    classDef security fill:#DC143C,stroke:#8B0000,stroke-width:2px,color:#fff,font-size:20px
    classDef quality fill:#2E8B57,stroke:#1B5E20,stroke-width:2px,color:#fff,font-size:20px
    classDef skill fill:#FFF3CD,stroke:#FFC107,stroke-width:1px,color:#000,font-size:20px
    classDef target fill:#E8F5E9,stroke:#4CAF50,stroke-width:2px,color:#000,font-size:20px
    classDef user fill:#1E88E5,stroke:#0D47A1,stroke-width:3px,color:#fff,font-weight:bold,font-size:20px

    DEV["👨‍💻 Developer — Natural Language Intent"] ==> ORCH
    ORCH["🎯 @dev-orchestrator\nRoutes tasks • Executes multi-agent workflows"]

    subgraph BUILD["🔨 BUILD & DEVELOP"]
        A1["@java-api-dev\nEndpoints, Services, DTOs"]
        A2["@new-api-scaffold\nFull resource in one pass"]
        A3["@api-modification\nSafely modify across layers"]
        A4["@sql-data\nOracle DDL, Sequences"]
    end

    subgraph SECURE["🛡️ SECURITY"]
        B1["@fortify-fix\nSAST — CWE remediation"]
        B2["@mend-fix\nSCA — Dependency CVE fixes"]
    end

    subgraph QUALITY["✅ QUALITY & TEST"]
        C1["@testing\nJUnit 5, MockMvc"]
        C2["@code-review\nReview + Fix (interactive)"]
        C3["@perf-optimizer\nN+1, HikariCP, Caching"]
        C4["@bug-fix\nDiagnose + Fix"]
    end

    ORCH --> BUILD
    ORCH --> SECURE
    ORCH --> QUALITY

    subgraph KNOWLEDGE["📚 Backing Knowledge"]
        K1["17 Skills"] --- K2["35 Prompts"] --- K3["OWASP <br>• CWE<br>• CISA KEV"]
    end

    BUILD -.-> KNOWLEDGE
    SECURE -.-> KNOWLEDGE
    QUALITY -.-> KNOWLEDGE

    subgraph TARGET["🎯 PROJECT OUTPUT"]
        T1["src/main/java"] --- T2["src/test/java\n≥80% coverage"] --- T3["pom.xml\nZero Critical/High CVEs"]
    end

    BUILD ==> T1
    QUALITY ==> T2
    SECURE ==> T3

    class DEV user
    class ORCH orchestrator
    class A1,A2,A3,A4 dev
    class B1,B2 security
    class C1,C2,C3,C4 quality
    class K1,K2,K3 skill
    class T1,T2,T3 target
```

---

## Slide 2: Workflow Pipelines — How Agents Chain Together

```mermaid
flowchart LR
    classDef phase fill:#4169E1,stroke:#1E3A8A,stroke-width:2px,color:#fff
    classDef security fill:#DC143C,stroke:#8B0000,stroke-width:2px,color:#fff
    classDef quality fill:#2E8B57,stroke:#1B5E20,stroke-width:2px,color:#fff
    classDef docs fill:#9370DB,stroke:#4B0082,stroke-width:2px,color:#fff

    subgraph W1["🆕 New API Resource (End-to-End)"]
        direction LR
        W1A["@sql-data"] ==> W1B["@new-api-scaffold"] ==> W1C["@testing"] ==> W1D["@code-review"] ==> W1E["@doc-gen"]
    end

    subgraph W2["✏️ Modify Existing API"]
        direction LR
        W2A["@api-modification"] ==> W2B["@testing"] ==> W2C["@code-review"]
    end

    subgraph W3["🛡️ Security Remediation"]
        direction LR
        W3A["@fortify-fix\nSAST"] ==> W3B["@mend-fix\nSCA"] ==> W3C["@testing"] ==> W3D["@code-review"]
    end

    subgraph W4["🐛 Bug Fix"]
        direction LR
        W4A["@bug-fix"] ==> W4B["@testing"] ==> W4C["@code-review"]
    end

    subgraph W5["⚡ Performance"]
        direction LR
        W5A["@perf-optimizer"] ==> W5B["@testing"] ==> W5C["@code-review"]
    end

    class W1A,W1B,W2A,W4A,W5A phase
    class W3A,W3B security
    class W1C,W1D,W2B,W2C,W3C,W3D,W4B,W4C,W5B,W5C quality
    class W1E docs
```

---

## Key Talking Points

| Principle | Implementation |
|-----------|---------------|
| **Single Responsibility** | Each agent owns one concern (build, test, secure, review, document) |
| **Chain of Verification** | Every workflow ends with `@testing` → `@code-review` |
| **Security by Default** | `@fortify-fix` (SAST) + `@mend-fix` (SCA) aligned to OWASP 2025 / CISA KEV |
| **Knowledge-Backed** | 17 skills + 35 prompts provide domain expertise — no hallucination |
| **Minimal Change** | Agents apply smallest fix — no speculative refactoring |
| **Human-in-the-Loop** | Developer provides intent → agents execute → `@code-review` validates |
