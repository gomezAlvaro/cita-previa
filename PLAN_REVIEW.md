# Plan Review: SEPE Cita Previa

_Review date: 2026-09-29, against `main` @ `6ed113c`_

This review covers the two planning documents (`PROJECT_PLAN.md`, `SEPE_MVP_PLAN.md`) and checks them against the code that actually exists in `sepe-mvp/`.

---

## 1. Summary

| | Verdict |
|---|---|
| **`PROJECT_PLAN.md`** (full platform) | Too big for where the project is. 13+ weeks, 6 services (Postgres, Redis, RabbitMQ, Selenium, email/SMS, admin), before there is proof that availability can even be detected. Keep it as a long-term vision, not a roadmap. |
| **`SEPE_MVP_PLAN.md`** (slot checker) | Right direction and the right size. But the code has drifted away from it, and its core technical assumption (that slot availability can be read from SEPE's site) has not been validated yet. |
| **Code in `sepe-mvp/`** | Does not implement either plan. It checks whether the SEPE portal homepage answers a `HEAD` request and presents that as appointment availability. The result is the same for every province, and office names, addresses and distances are placeholders. |

**Main recommendation:** before building more UI or infrastructure, do a 1-day spike to prove that the real availability signal can be obtained from SEPE's cita previa flow. Everything else in both plans depends on it.

---

## 2. The two plans disagree with each other and with the code

| Topic | `PROJECT_PLAN.md` | `SEPE_MVP_PLAN.md` | Actual code |
|---|---|---|---|
| Goal | Auto-book appointments | Show if slots exist; user books manually | Show if the SEPE portal is reachable |
| User input | Account + NIE/NIF + preferences | Province + office dropdowns | DNI + postal code + type |
| Endpoints | Many (auth, bookings, admin) | `GET /api/provinces`, `POST /api/check-slots` | `GET /api/status`, `POST /api/find-appointments` |
| SEPE access | Selenium/Playwright, form filling | Playwright Java, parse page text | `java.net.http` `HEAD` on `https://sede.sepe.gob.es/portalSede` |
| Office data | DB table | `offices.json` | Hardcoded placeholders ("Oficina Principal X", "Calle Principal") |
| Styling | Vue + TS | Vue + Tailwind | Vue (JS, plain CSS) |
| Storage | Postgres + Redis | None | Caffeine cache (60 s) |

**Action:** pick one definition of the MVP and update `SEPE_MVP_PLAN.md` to match it. The postal-code flow in the current code is closer to how users think ("near me") than province/office dropdowns. If you keep it, the plan should say so and define its endpoints.

---

## 3. Issues in the current code (by severity)

### Critical: the core feature doesn't measure what it claims

1. **Availability = homepage is up.** `checkProvinceStatus()` and `checkProvince()` both send `HEAD` to the same URL (`/portalSede`) and ignore the province code. So:
   - All 51 provinces always show the same status.
   - `hasAppointments: true` only means "the portal returned 200", not that any appointment exists.
   - The "nearby provinces with availability" feature can never find a difference between provinces.
2. **Fabricated office data is shown to users as real.** Office names, the address `"Calle Principal, <CP>"`, and distances computed from the *difference between province codes* (`calculateDistance`) are presented as facts in the UI. A user could travel to an office that doesn't exist.

### High: the app doesn't run as documented

3. **Port mismatch.** The backend listens on `8080` (`application.properties`). The Vite proxy targets `8081`, and the README says `8081`. The frontend therefore always shows "No se pudo conectar con el servidor."
4. **`./mvnw` is broken.** The committed `mvnw` is a hand-written script, not the official wrapper:
   - it resolves the base dir to the **parent** folder (`sepe-mvp/`), not `backend/`;
   - it runs `java -jar maven-wrapper.jar`, but that jar has no `Main-Class`. It fails with `no main manifest attribute`.
   
   Fix: regenerate with `mvn wrapper:wrapper` (or document `mvn spring-boot:run` instead).
5. **Wrong province codes.** In `PROVINCES`, `48` is labelled Zamora and `49` Zaragoza. Correct values are `48` = Bizkaia, `49` = Zamora and `50` = Zaragoza. Consequences:
   - Postal codes `50xxx` (Zaragoza) resolve to "Desconocida".
   - Bilbao users (`48xxx`) are told they're in Zamora.

### Medium

6. **DNI is collected and sent, but never used.** The form asks for a national ID number and POSTs it to the backend, which ignores it. Either remove the field, or, if the real SEPE check turns out to need it (see §4), handle it deliberately: don't log it, don't cache it, and say so in the UI.
7. **`appointmentType` is ignored** by the backend too.
8. **Slow worst case.**
   - `GET /api/status` makes 51 sequential requests with 3 s timeouts each, so up to about 150 s on a cold cache.
   - When no province matches, `find-appointments` loops over 51 more requests without caching.
   - Once real per-province checks exist, make them parallel (and rate-limited).
9. **No throttling** on either endpoint. Anyone hitting the API makes the server hit SEPE. The MVP plan's "5 s delay between checks" isn't implemented.
10. **`POSTAL_CODE_TO_PROVINCE`**: 18,000 map entries that `inferProvinceFromPostalCode()` (first two digits) already covers. It can be deleted.
11. **No error distinction.** Timeouts, 403s (bot blocking) and CAPTCHAs all collapse into `false`/`UNKNOWN`. The MVP plan asks for a distinct CAPTCHA message.

### Low / housekeeping

12. `sepe-mvp/backend/target/` (compiled classes) is committed even though `.gitignore` excludes `target/`. Remove it with `git rm -r --cached`.
13. Stray `touch` file at the repo root (contents: `hi`). It looks accidental.
14. Duplicate `maven-wrapper.jar` in `sepe-mvp/.mvn/` and `sepe-mvp/backend/.mvn/`.
15. No tests at all. The MVP plan doesn't mention any either.
16. There is no `docker-compose.yml`, although the MVP plan's success criteria require `docker-compose up`.

---

## 4. The unvalidated assumption (do this first)

Both plans assume SEPE's cita previa pages will reveal slot availability to an automated client. That is the whole product, and nothing in the repo tests it yet. Open questions to answer in a spike:

- **Flow:** what does the real cita previa flow require before it shows availability? Postal code only, or DNI/NIE too? Is there a session, a token or a CAPTCHA step?
- **Signal:** what exactly signals "no slots" vs "slots"? The strings in the MVP plan (`"No hay citas disponibles"`, `"Seleccione día y hora"`) are guesses and need to be confirmed against the live site.
- **Mechanism:** can plain HTTP handle it (cheap, fast), or does it need a headless browser (Playwright: heavier, slower, bigger Docker image)?
- **Blocking:** how quickly does SEPE rate-limit or block repeated checks from one IP?

**Deliverable of the spike:** a single `SepeSlotService.check(postalCode)` method that returns `AVAILABLE / NONE / BLOCKED / ERROR` against the live site, plus a note on the request rate that is safe.

If the spike shows availability can't be read reliably, the product becomes a better *manual* helper instead: correct office lookup by postal code, direct deep links and reminders. That is still useful and cheaper.

---

## 5. Suggested revised MVP roadmap

| Step | Scope | Est. |
|---|---|---|
| 0 | Housekeeping: fix ports, replace `mvnw`, fix province codes, remove `target/` and `touch`, drop the unused DNI field | 0.5 d |
| 1 | **Spike**: real availability detection for 1 province (§4) | 1–2 d |
| 2 | Real office data (`offices.json` or scraped list) keyed by postal code, replacing placeholders | 1 d |
| 3 | Per-province check wired to `find-appointments`, with cache (60 s) + server-side rate limit | 1 d |
| 4 | UI: clear states (available / none / SEPE blocked / error), timestamp, optional 30–60 s auto-refresh, "Reservar en SEPE" link | 1 d |
| 5 | Tests for postal-code → province, result parsing (saved HTML fixtures), controller | 0.5 d |
| 6 | Dockerfile + `docker-compose.yml`, README update | 0.5 d |

The following stay out until the MVP proves useful, in line with `SEPE_MVP_PLAN.md`: accounts, database, queues, notifications and auto-booking.

---

## 6. Notes on `PROJECT_PLAN.md`

- **Auto-booking is the riskiest part.** It is the most likely to get blocked or to break with any site change. Spain has also moved against automated booking of administrative appointments, especially where slots end up being resold. Before investing in Phase 2, check SEPE's current terms and the regulations that apply.
- **Sensitive data.** It stores NIE/NIF and would need SEPE credentials or personal data per user. That brings GDPR obligations: a legal basis, encryption at rest, retention limits and a DPA with any SMS/email provider. The Security section should cover these explicitly.
- **Infrastructure.** The target metrics (booking success > 60%, 99.5% uptime) and the infra list (RabbitMQ, ELK, Prometheus) are premature. Revisit them after the MVP has real users.
- **Queue code.** The example uses `Thread.sleep` inside a `@RabbitListener` with backoff up to about 85 min, which would block consumer threads. If a queue is ever built, use delayed re-queueing instead.

---

## 7. Verification notes

- Backend compilation could not be verified in the review environment (Maven Central was not reachable). The code was reviewed by reading.
- Nothing was tested against the live SEPE site.
