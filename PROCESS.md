# PROCESS.md — how this app was built

## Plan

I worked with an AI coding agent (Devin/"HAL"). Before any code was written I
made the agent interview me — one question at a time — until the stack and
approach were explicit: Maven + Java 25 + Spring Boot 4.2, Thymeleaf UI, SQLite
via JPA, the mocked bureau as an HTTP endpoint inside the same app (real
HTTP → realistic timeout/failure semantics), a hidden-UUID idempotency key for
duplicate-submit protection, and small `[ai]`-tagged commits. The agent stated
the plan back and I approved it before it started.

## Workflow step by step

1. **"Log our whole interaction"** — my first prompt set up
   `AI-Interactions.txt` as a running session log (request verbatim + action
   summary per entry). Approved format via three clarifying Q&As.
2. **"Create the app" (pointing at the PRD)** — the agent extracted the PDF
   with macOS PDFKit, interviewed me (5 questions), then scaffolded the whole
   app: Maven pom, entity, repository, mock bureau endpoint, RestClient with
   2s timeout, decision service, Thymeleaf pages, and 7 test classes.
3. I accepted the overall architecture as-is. All debugging below was done by
   watching the agent run `mvn test` and live `curl` checks.
4. Agent wrote README and this file; I reviewed.

## Places where the AI was wrong or went off track

1. **Spring Boot 4 APIs guessed from Boot 3 knowledge.** It used
   `org.springframework.boot.web.context.WebServerApplicationContext` and
   `org.springframework.boot.test.web.client.TestRestTemplate` — both moved in
   Boot 4 (`web.server.context`, and `spring-boot-resttestclient` needing
   `spring-boot-restclient` + `@AutoConfigureTestRestTemplate`). Caught by
   compilation errors; fixed after inspecting the actual jars in `~/.m2`.
2. **`GenerationType.IDENTITY` silently breaks on SQLite + Hibernate 7.**
   Hibernate emitted `id` with no column type, so the column wasn't a rowid
   alias: inserts left `id` NULL, `last_insert_rowid()` returned the rowid,
   and every keyed lookup returned the row but was discarded for a null id —
   surfacing as a UNIQUE-constraint violation on the second idempotent submit.
   This was the hardest bug; the agent proved it via SQL/binding tracing and a
   JDBC cross-check, then switched to `GenerationType.AUTO` (sequence-table
   generator). I'd have assumed IDENTITY was safe — good catch.
3. **`application` is a reserved implicit object in Thymeleaf.** The detail
   model attribute `application` was shadowed by the application-scope map, so
   every `application.x` rendered empty and `status.name()` blew up on null.
   The agent first patched the expression (`T(...)` enum compare) which did
   nothing; the real fix was renaming the attribute to `app`. My takeaway:
   don't name model attributes `application`, `session`, `param`, or `request`.

## What I wrote or fixed by hand

<!-- Troy: edit this section — e.g., what you reviewed/changed manually,
     approval decisions, any template or rule tweaks you made yourself. -->
- Chose all key design decisions (stack, bureau-as-HTTP-endpoint, UUID
  idempotency, commit cadence) through the interview.
- Hand edits: _[fill in anything you changed yourself]_

## How I checked that it works

- `mvn test` → 28 tests, 0 failures (incl. the 3 PRD-required tests).
- Live smoke test with `curl`: submitted the same form twice with one
  idempotency key → same record, bureau billed once; forced Pending rows via
  the ~20% failure rate → detail page → POST `/applications/{id}/retry` →
  Approved with a fresh score; verified `**-***6789` masking, status filter,
  and the call-count/cost footer.

## Time + what I'd do with another hour

Roughly _[fill in]_ elapsed. With another hour: a small CSS/component pass,
concurrent-submit test (two threads, same key), and a `bureau.enabled` toggle
for demoing failure-free runs in the video.
