# How every file is written (my study format)

Every question file keeps its original sections (PROBLEM, ALGORITHM, APPROACHES, WHY THIS ONE,
EXPECTED OUTPUT). On top of that, each file has the blocks below. They follow the way I prepare:
understand it in Hinglish, say it out loud in English, know the trap, and have an answer ready
for every follow-up.

| Block | Language | What it is for |
|---|---|---|
| `PRIORITY` | Hinglish | P1 / P2 / P3, so I know what to do first |
| `SAMJHO` | Hinglish | The idea in 2-4 short lines, the way I would explain it to a friend |
| `AISE BOLO` | English | 30-40 seconds of what I say in the interview, first person |
| `TRAP` | Hinglish | The mistake that costs marks, one or two lines |
| `FOLLOW-UPS KE JAWAB` | English question, Hinglish answer | Every follow-up has an answer, not just the question |
| `JAVA 21 MEIN` (Java, optional) | Hinglish | Only when a newer API really changes the answer |
| `ORACLE / POSTGRES MEIN` (SQL, optional) | Hinglish | Only when the syntax differs from what the file runs on SQLite |
| `APNI STORY SE JODO` (optional) | English line + Hinglish note | Only when one of my real work facts fits. Never invented |
| `VARIANTS` | - | LeetCode numbers or classic variants, only when sure |

## Priority

| Tier | Meaning | How to study |
|---|---|---|
| **P1** | Comes in almost every 8-year Java backend round in India: service companies, product companies, GCCs | Type from memory, twice |
| **P2** | Common in product, fintech and GCC rounds | Type once, then say AISE BOLO out loud |
| **P3** | Occasional, or data-engineering flavour | Read SAMJHO and the trap; type only if time is left |

## Layout inside a Java header

```
 * TRACK        : ...
 * DIFFICULTY   : ...
 * WEIGHTAGE    : ...
 * PRIORITY     : P1   -- one Hinglish reason
 * TECHNIQUE    : ...
 *
 * PROBLEM ... ALGORITHM ... APPROACHES ... WHY THIS ONE      (unchanged)
 *
 * SAMJHO
 *   Hinglish, 2-4 lines.
 *
 * AISE BOLO  (30-40 sec)
 *   "English, first person, 3-5 sentences: brute force and why not, the approach,
 *   time and space, one edge case or trade-off."
 *
 * TRAP
 *   ! Hinglish, 1-2 lines. A second ! line if there is a second trap.
 *
 * FOLLOW-UPS KE JAWAB
 *   ? <the original English follow-up, unchanged>
 *     -> Hinglish answer, 1-2 lines
 *
 * LEARN THIS AND YOU ALSO GET                                (unchanged)
 *
 * JAVA 21 MEIN            (optional)
 * APNI STORY SE JODO      (optional)
 * VARIANTS     : LC 1, LC 167
 * ==============================================================================================
```

SQL files use the same blocks with `--` instead of ` * `, and `ORACLE / POSTGRES MEIN` in place of
`JAVA 21 MEIN`. `FOLLOW-UPS` becomes `FOLLOW-UPS KE JAWAB`, and `ALSO UNLOCKS` stays.

## Writing rules

- **Hinglish** means Hindi in Roman script mixed with English technical words, in short and simple
  sentences: "HashMap mein value se index rakho, ek hi pass mein jawab mil jata hai." No Devanagari.
- **AISE BOLO** is English, spoken, first person. It names the brute force, then the chosen
  approach with time and space, then one edge case or trade-off.
- ASCII only inside code files: no curly quotes, no long dashes, no arrows other than `->`.
- Lines stay within 100 characters, inside the existing `====` border.
- A follow-up answer that needs code points to the section of the file that has it.
- Code and EXPECTED OUTPUT change only to fix a real bug. Then the file is run again and the new
  output is pasted exactly.

## APNI STORY SE JODO: what is allowed

This repo is public, so stories are written the way my LinkedIn profile is: no company, product or
client names, and no internal details. Only these facts may be used. Anything else is a design
answer, or a `[bracket]` for me to fill with what is true.

1. 8 years in Java backend, mostly fintech and remittance. Senior Software Engineer doing the
   technical lead work on a team of 5 developers, 2 report to me.
2. Built an event-driven file-processing platform from scratch as sole architect and developer:
   8 microservices on Azure Service Bus, Spring Boot, PostgreSQL, AKS. `@Scheduled` batch jobs with
   retry with backoff, Service Bus duplicate detection and dead-letter queue review. Live in
   production.
3. A cloud migration in a team of 3: 7 production microservices from Java 1.7 directly to Java 21
   and Spring Boot 3, from on-premises to AKS, in about 6 months. The biggest break was javax to
   jakarta. ActiveMQ was replaced by Azure Service Bus through the JMS library.
4. A daily reconciliation query on an Oracle table of more than 10 million rows: 45 minutes to
   8 seconds. The EXPLAIN plan showed a full table scan, and the fix was a missing index.
5. A batch window: 45 minutes to 8 minutes, by replacing a synchronized shared HashMap with
   ConcurrentHashMap to remove lock contention.
6. Also used at work: Java 21 virtual threads, Redis, a rate limiter, Keycloak with OAuth2/JWT,
   Application Insights, Azure DevOps CI/CD with SonarQube, OWASP dependency check and Trivy.
7. Earlier job: SAML SSO with PingIdentity, Okta and Azure AD for an enterprise legal product; a
   multi-tenant (tenant_id) spa management platform with Stripe payments; Elasticsearch.
8. AWS: hands-on through my own projects, not on a company project. At work the cloud is Azure.

**Never write:** company, product or client names; Kafka in production; AWS on a company project;
Resilience4j in production; a production deadlock I fixed; "1M+ per batch"; any number that is not
in the list above.
