# Clicklease — Mini Lease Application

Take-home exercise: a small business owner applies for an equipment lease, the
app calls a (mocked) credit bureau, makes a decision, and persists everything
to SQLite.

## Stack

- Java 25, Spring Boot 4.2.0, Maven
- Thymeleaf server-rendered frontend
- Spring Data JPA + SQLite (`xerial` JDBC + `hibernate-community-dialects`)

## Run the app

```bash
mvn spring-boot:run
```

Then open <http://localhost:8080> — submit the form, see the decision, browse
the Applications list (filterable by status), open a detail page, and use
**Retry decision** on Pending applications. The SQLite file lives at
`./data/lease.db` and survives restarts.

## Run the tests

```bash
mvn test
```

28 tests — covers the three required cases (decision boundaries 649/650 and
25,000/25,001, bureau failure **and** timeout → Pending, duplicate submission →
single bureau call) plus EIN masking, bureau score determinism, and inline
form validation.

## Mock credit bureau

`GET /mock/bureau/score?taxId=XX-XXXXXXX` lives inside the same app and is
called over real HTTP by `BureauClient` (2s read timeout). It returns a
deterministic score 300–850 per tax ID after a random 200 ms–3 s delay, fails
~20% of the time, and counts every call at $1.50 — see `GET /mock/bureau/stats`
or the footer of the Applications page. Behavior is tunable via the
`bureau.*` properties in `src/main/resources/application.properties`.

## Notes

- **Idempotency**: each form render carries a hidden UUID; a unique DB
  constraint plus a check-then-act service guarantees the paid bureau call
  happens at most once per submission (double-click/refresh safe).
- **Sensitive data**: the EIN is stored in the DB but never rendered or logged
  in full — only `**-***1234` (last four digits).
- `AI-Interactions.txt` logs the full AI-assisted build session; `PROCESS.md`
  describes the workflow.
