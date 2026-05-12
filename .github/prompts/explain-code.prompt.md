---
description: "Use when: explaining code, understanding a class or method, or onboarding to unfamiliar code — provides a structured walkthrough of what the code does and why."
agent: "agent"
tools: [read, search]
argument-hint: "Class name or file path — e.g., 'OrderServiceImpl' or 'What does the GlobalExceptionHandler do?'"
---

# Explain Code

Provide a clear, structured explanation of the specified Java Spring Boot code.

## What to Cover

### 1. Purpose
- What is this class/method responsible for?
- Where does it fit in the application architecture? (controller / service / repository / config / etc.)

### 2. Dependencies
- What classes does it depend on? (injected via constructor)
- What annotations configure its behavior?

### 3. Method-by-Method Walkthrough
For each public method:
- **What it does** in plain language
- **Input** — parameters and their purpose
- **Output** — return type and what it represents
- **Side effects** — database writes, external calls, exceptions thrown
- **Transaction behavior** — is it transactional? read-only?

### 4. Key Design Decisions
- Why was it designed this way?
- What patterns are being used? (repository pattern, strategy, specification, etc.)
- Any notable Spring Boot / JPA features in use?

### 5. Relationships
- What calls this code? (upstream callers)
- What does this code call? (downstream dependencies)

## Output Format

Use clear headings, bullet points, and code snippets. Write for a developer who is new to this codebase but familiar with Spring Boot.

Do NOT suggest changes — this is explanation only.
