# SEPE Cita Previa MVP - Simplified Plan

## Goal
A minimal webapp that helps users **find available appointment slots** on SEPE faster than manual refreshing. No automation, no booking, no queues.

---

## Architecture (Minimal)
- **Backend**: Java 17 + Spring Boot 3 (single JAR)
- **Frontend**: Vue.js 3 (Vite, single-page app)
- **Database**: None (or SQLite if you need to store user sessions)
- **Deployment**: Run locally or simple Docker container

---

## Single Feature: Slot Availability Checker

### What it does:
1. User selects their province and office
2. Backend checks SEPE website for available slots (via simple HTTP calls or lightweight browser session)
3. Frontend displays results: "Available" / "Not Available" with timestamp
4. User manually clicks through to book on SEPE's site

### What it DOESN'T do:
- ❌ No auto-booking
- ❌ No user accounts
- ❌ No notifications
- ❌ No job queues
- ❌ No database (optional: only for session tracking)
- ❌ No admin panel

---

## Tech Stack (Minimal)

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3 + WebClient (for HTTP calls) |
| Frontend | Vue 3 + Vite + TailwindCSS |
| Automation | Playwright Java (lightweight, headless) |
| Config | application.yml (provinces/offices list) |

---

## Project Structure

```
sepe-mvp/
├── backend/
│   ├── src/main/java/com/sepe/mvp/
│   │   ├── SepeApplication.java
│   │   ├── controller/SlotController.java
│   │   ├── service/SepeSlotService.java
│   │   └── config/PlaywrightConfig.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── offices.json (province → office mapping)
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── App.vue
│   │   ├── components/SlotChecker.vue
│   │   └── assets/
│   ├── package.json
│   └── vite.config.js
│
├── docker-compose.yml (optional)
└── README.md
```

---

## API Endpoints (Only 2)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/provinces` | Return list of provinces |
| POST | `/api/check-slots` | Body: `{province, office}` → Returns `{available: boolean, timestamp}` |

---

## Development Steps (3-5 days)

### Day 1: Setup & Backend Skeleton
- [ ] Initialize Spring Boot project
- [ ] Create `offices.json` with province/office data
- [ ] Build `/api/provinces` endpoint
- [ ] Test with curl/Postman

### Day 2: Slot Checking Logic
- [ ] Integrate Playwright Java
- [ ] Write `SepeSlotService` to navigate SEPE site
- [ ] Implement slot detection logic (parse HTML for "available" indicators)
- [ ] Build `/api/check-slots` endpoint
- [ ] Handle errors (SEPE down, CAPTCHA, timeouts)

### Day 3: Frontend
- [ ] Initialize Vue 3 + Vite project
- [ ] Create dropdowns for province/office selection
- [ ] Add "Check Availability" button
- [ ] Display result with timestamp
- [ ] Add auto-refresh option (every 30s, user-controlled)

### Day 4: Polish & Test
- [ ] Add loading states
- [ ] Error handling (SEPE unavailable, network issues)
- [ ] Basic styling (TailwindCSS)
- [ ] Test with real SEPE site
- [ ] Write README with setup instructions

### Day 5 (Optional): Docker & Deployment
- [ ] Create Dockerfile for backend
- [ ] Multi-stage build for frontend + backend
- [ ] Test local deployment
- [ ] Document deployment steps

---

## Key Implementation Details

### 1. Office Data (`offices.json`)
```json
{
  "Madrid": ["Madrid Capital", "Alcalá de Henares", "Móstoles"],
  "Barcelona": ["Barcelona Capital", "Hospitalet", "Badalona"],
  ...
}
```

### 2. Slot Detection Logic
- Navigate to SEPE cita previa URL with province/office params
- Look for specific HTML elements indicating availability:
  - `"No hay citas disponibles"` → `available: false`
  - `"Seleccione día y hora"` → `available: true`
- Return immediately (no waiting, no retries in MVP)

### 3. Frontend UX
```
[Province Dropdown] → [Office Dropdown] → [Check Button]
                                      ↓
                    Result: ✅ Available (14:32:05)
                            ❌ Not Available (14:32:05)
                    [Auto-refresh: ⬜ Every 30s]
                    [Book Now →] (links to SEPE)
```

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| SEPE changes website structure | Keep selector logic modular, easy to update |
| CAPTCHA appears | Show error: "CAPTCHA detected, try manually" |
| Rate limiting from SEPE | Add 5-second delay between checks, warn users |
| Legal concerns | Add disclaimer: "For educational purposes only" |

---

## Success Criteria (MVP)
- ✅ User can select province/office
- ✅ System checks SEPE and returns availability in <10 seconds
- ✅ Frontend displays clear result with timestamp
- ✅ User can manually click through to book
- ✅ Runs locally with `docker-compose up` or `java -jar`

---

## Next Steps (Post-MVP)
If this works and users want more:
1. Add email/SMS notifications
2. Add auto-refresh server-side (with rate limiting)
3. Add user sessions to track preferences
4. Add multiple province monitoring

---

## Disclaimer
> This tool is for **personal use only**. Respect SEPE's terms of service. Do not use aggressive polling that could impact their infrastructure. This MVP does not automate booking—users must complete the process manually on SEPE's website.
