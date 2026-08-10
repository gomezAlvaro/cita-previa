# SEPE Cita Previa MVP

Simple status checker for SEPE appointment booking system.

## Structure
- `backend/` - Spring Boot 3 Java application
- `frontend/` - Vue 3 + Vite application

## Run Backend
```bash
cd backend
./mvnw spring-boot:run
# or
mvn spring-boot:run
```

Backend runs on http://localhost:8080

API Endpoint: `GET /api/status`

## Run Frontend
```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:5173

## Features
- Checks all 52 Spanish provinces
- Shows status: Available (green), Busy (red), Unknown (yellow)
- Response time monitoring
- Direct links to official SEPE booking pages
- 60-second cache to prevent rate limiting

## How it Works
The backend makes HTTP HEAD requests to SEPE's main portal and evaluates:
- **Available**: 200 OK + response < 2s
- **Busy**: 5xx errors or response > 5s
- **Unknown**: Other responses or timeouts

No automation, no database, no browser drivers - just simple HTTP health checks.
