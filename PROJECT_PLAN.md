# SEPE Cita Previa Automation - Project Plan

## Executive Summary
A web application to automate the process of booking appointments ("cita previa") with Spain's Public Employment Service (SEPE), addressing the current difficulty users face with the official system.

---

## 1. Architecture Overview

### Tech Stack
- **Backend**: Java 17+ with Spring Boot 3.x
- **Frontend**: Vue.js 3 with TypeScript
- **Database**: PostgreSQL (primary) + Redis (caching/sessions)
- **Browser Automation**: Selenium WebDriver or Playwright
- **Task Queue**: RabbitMQ or Apache Kafka
- **Containerization**: Docker & Docker Compose

### High-Level Architecture
```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Vue.js    │────▶│ Spring Boot  │────▶│   SEPE      │
│  Frontend   │◀────│   Backend    │     │   Website   │
└─────────────┘     └──────────────┘     └─────────────┘
                          │
                    ┌─────▼─────┐
                    │ PostgreSQL│
                    │  + Redis  │
                    └───────────┘
                          │
                    ┌─────▼─────┐
                    │RabbitMQ   │
                    │(Job Queue)│
                    └───────────┘
```

---

## 2. Core Features

### 2.1 User Management
- User registration/login (email + password)
- OAuth2 integration (Google, Microsoft)
- Profile management
- Multi-user support with role-based access (admin, user)

### 2.2 Appointment Booking Engine
- **Automated Form Filling**
  - Personal data (NIE/NIF, name, surname)
  - Contact information (phone, email)
  - Province and office selection
  - Appointment type selection
  - Preferred date/time slots
  
- **Smart Scheduling**
  - Continuous monitoring for available slots
  - Priority-based booking (users can set urgency levels)
  - Multiple retry attempts with exponential backoff
  - Timezone-aware scheduling

### 2.3 Notification System
- Email notifications (booking confirmation, status updates)
- SMS alerts (via Twilio or similar)
- Push notifications (Web Push API)
- Telegram/WhatsApp bot integration (optional)

### 2.4 Dashboard
- Real-time booking status
- Appointment history
- Success/failure analytics
- Queue position tracking
- Manual intervention interface

### 2.5 Admin Panel
- User management
- System health monitoring
- Booking success rate metrics
- Rate limiting configuration
- Audit logs

---

## 3. Technical Implementation Details

### 3.1 Backend Structure (Spring Boot)

```
src/main/java/com/sepecita/
├── config/
│   ├── SecurityConfig.java
│   ├── SeleniumConfig.java
│   ├── RabbitMQConfig.java
│   └── SchedulerConfig.java
├── controller/
│   ├── AuthController.java
│   ├── BookingController.java
│   ├── UserController.java
│   └── AdminController.java
├── service/
│   ├── BookingService.java
│   ├── SepeAutomationService.java
│   ├── NotificationService.java
│   ├── UserService.java
│   └── QueueService.java
├── repository/
│   ├── UserRepository.java
│   ├── BookingRepository.java
│   └── AppointmentRepository.java
├── model/
│   ├── User.java
│   ├── BookingRequest.java
│   ├── Appointment.java
│   └── BookingStatus.java
├── dto/
│   ├── request/
│   └── response/
├── exception/
│   └── GlobalExceptionHandler.java
└── automation/
    ├── SepeBot.java
    ├── BrowserManager.java
    ├── FormFiller.java
    └── SlotFinder.java
```

### 3.2 Frontend Structure (Vue.js 3 + TypeScript)

```
src/
├── components/
│   ├── auth/
│   │   ├── LoginForm.vue
│   │   └── RegisterForm.vue
│   ├── booking/
│   │   ├── BookingForm.vue
│   │   ├── ProvinceSelector.vue
│   │   ├── OfficeSelector.vue
│   │   ├── DateTimePicker.vue
│   │   └── StatusTracker.vue
│   ├── dashboard/
│   │   ├── BookingList.vue
│   │   ├── StatisticsChart.vue
│   │   └── NotificationPanel.vue
│   └── common/
│       ├── LoadingSpinner.vue
│       ├── AlertMessage.vue
│       └── ModalDialog.vue
├── views/
│   ├── HomeView.vue
│   ├── LoginView.vue
│   ├── DashboardView.vue
│   ├── NewBookingView.vue
│   ├── HistoryView.vue
│   └── AdminView.vue
├── stores/
│   ├── auth.ts
│   ├── booking.ts
│   └── notifications.ts
├── router/
│   └── index.ts
├── api/
│   ├── auth.ts
│   ├── booking.ts
│   └── user.ts
├── types/
│   ├── index.ts
│   └── models.ts
└── utils/
    ├── validators.ts
    └── formatters.ts
```

### 3.3 Database Schema

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    nie_nif VARCHAR(20) NOT NULL,
    phone VARCHAR(20),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Booking requests
CREATE TABLE booking_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    province VARCHAR(100) NOT NULL,
    office_code VARCHAR(50),
    appointment_type VARCHAR(100) NOT NULL,
    preferred_dates DATE[],
    priority_level INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Appointments
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_request_id UUID REFERENCES booking_requests(id),
    confirmation_code VARCHAR(50) UNIQUE,
    appointment_date TIMESTAMP NOT NULL,
    office_name VARCHAR(255),
    office_address TEXT,
    status VARCHAR(50) DEFAULT 'CONFIRMED',
    booked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

-- Audit log
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 3.4 Automation Strategy

#### Approach 1: Browser Automation (Recommended for MVP)
```java
@Service
public class SepeAutomationService {
    
    @Autowired
    private WebDriver driver;
    
    public BookingResult bookAppointment(BookingRequest request) {
        try {
            // Navigate to SEPE website
            driver.get("https://sede.sepe.gob.es/");
            
            // Navigate to cita previa section
            waitForElement(By.id("cita-previa-link")).click();
            
            // Fill personal data form
            fillPersonalData(request.getUserData());
            
            // Select province and office
            selectProvinceAndOffice(request.getProvince(), request.getOfficeCode());
            
            // Select appointment type
            selectAppointmentType(request.getAppointmentType());
            
            // Find available slots
            List<LocalDateTime> availableSlots = findAvailableSlots(
                request.getPreferredDates()
            );
            
            if (availableSlots.isEmpty()) {
                return BookingResult.noSlotsAvailable();
            }
            
            // Book first available slot
            LocalDateTime selectedSlot = availableSlots.get(0);
            String confirmationCode = confirmBooking(selectedSlot);
            
            return BookingResult.success(confirmationCode, selectedSlot);
            
        } catch (Exception e) {
            log.error("Booking failed", e);
            return BookingResult.failure(e.getMessage());
        }
    }
    
    private List<LocalDateTime> findAvailableSlots(List<LocalDate> preferredDates) {
        // Implement smart slot finding algorithm
        // Check multiple times with random delays to avoid detection
        // Parse calendar widget for available dates/times
    }
}
```

#### Approach 2: API Reverse Engineering (Advanced)
- Analyze SEPE's internal APIs using browser DevTools
- Replicate API calls programmatically
- Handle authentication tokens and session management
- **Risk**: Higher chance of being blocked, requires constant maintenance

### 3.5 Queue System Architecture

```java
@Configuration
public class RabbitMQConfig {
    
    @Bean
    public Queue bookingQueue() {
        return new Queue("booking.requests", true);
    }
    
    @Bean
    public Queue notificationQueue() {
        return new Queue("notifications", true);
    }
    
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("sepe.exchange");
    }
}

@Service
public class QueueService {
    
    @RabbitListener(queues = "booking.requests")
    public void processBookingRequest(BookingRequest request) {
        // Retry logic with exponential backoff
        int maxRetries = 10;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            BookingResult result = sepeAutomationService.bookAppointment(request);
            
            if (result.isSuccess()) {
                notificationService.sendConfirmation(result);
                break;
            }
            
            retryCount++;
            long delay = (long) Math.pow(2, retryCount) * 5000; // Exponential backoff
            Thread.sleep(delay);
        }
    }
}
```

---

## 4. Security Considerations

### Critical Security Measures
1. **Data Encryption**
   - Encrypt sensitive user data (NIE/NIF, phone numbers) at rest
   - Use TLS 1.3 for all communications
   - Never store passwords in plain text (use BCrypt)

2. **Rate Limiting**
   - Implement per-user rate limiting
   - Add IP-based throttling
   - Prevent abuse of the automation system

3. **Compliance**
   - GDPR compliance for EU users
   - Clear terms of service
   - Data retention policies
   - Right to deletion

4. **Bot Detection Evasion** (Ethical Considerations)
   - Random delays between actions
   - Rotate user agents
   - Respect robots.txt where applicable
   - **Important**: This tool should help users, not overwhelm SEPE's systems

5. **Authentication & Authorization**
   - JWT tokens with short expiration
   - Refresh token rotation
   - Role-based access control (RBAC)
   - CSRF protection

---

## 5. Development Phases

### Phase 1: Foundation (Weeks 1-3)
- [ ] Set up Spring Boot project structure
- [ ] Configure database and migrations
- [ ] Implement user authentication
- [ ] Create basic Vue.js frontend
- [ ] Set up Docker development environment

### Phase 2: Core Automation (Weeks 4-6)
- [ ] Develop SEPE browser automation
- [ ] Implement form filling logic
- [ ] Create slot detection algorithm
- [ ] Build booking confirmation parser
- [ ] Add error handling and retries

### Phase 3: Queue & Scheduling (Weeks 7-8)
- [ ] Set up RabbitMQ
- [ ] Implement job queue system
- [ ] Add scheduled tasks for continuous monitoring
- [ ] Create priority-based processing

### Phase 4: Notifications & UI (Weeks 9-10)
- [ ] Integrate email service (SendGrid/Mailgun)
- [ ] Add SMS notifications
- [ ] Build real-time dashboard
- [ ] Implement WebSocket for live updates

### Phase 5: Testing & Optimization (Weeks 11-12)
- [ ] Write unit and integration tests
- [ ] Load testing
- [ ] Optimize automation speed
- [ ] Security audit
- [ ] User acceptance testing

### Phase 6: Deployment & Monitoring (Week 13+)
- [ ] Set up CI/CD pipeline
- [ ] Deploy to production (AWS/GCP/Azure)
- [ ] Configure monitoring (Prometheus + Grafana)
- [ ] Set up logging (ELK stack)
- [ ] Create backup strategy

---

## 6. Infrastructure Requirements

### Development Environment
```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: sepe_cita
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev
    
  redis:
    image: redis:7-alpine
    
  rabbitmq:
    image: rabbitmq:3-management
    
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
      - rabbitmq
    
  frontend:
    build: ./frontend
    ports:
      - "5173:5173"
```

### Production Environment
- **Compute**: 2-4 vCPU, 4-8 GB RAM (auto-scaling)
- **Database**: Managed PostgreSQL (AWS RDS, Google Cloud SQL)
- **Cache**: Managed Redis (ElastiCache, Memorystore)
- **Storage**: S3-compatible for files/logs
- **CDN**: Cloudflare for static assets
- **Monitoring**: Prometheus + Grafana + AlertManager

---

## 7. Risk Mitigation

### Technical Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| SEPE changes website structure | High | Abstract automation layer, quick iteration |
| CAPTCHA implementation | High | Integrate 2Captcha service or manual solving |
| Rate limiting by SEPE | Medium | Distributed IPs, respectful polling intervals |
| Browser detection | Medium | Use headless browsers with stealth plugins |

### Legal/Ethical Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Terms of Service violation | High | Consult legal counsel, transparent usage |
| Data privacy concerns | High | GDPR compliance, minimal data collection |
| System abuse | Medium | Strict rate limiting, user verification |

---

## 8. Success Metrics

### Key Performance Indicators (KPIs)
- **Booking Success Rate**: Target > 60%
- **Average Time to Book**: < 5 minutes from request
- **System Uptime**: > 99.5%
- **User Satisfaction**: > 4.5/5 rating
- **False Positive Rate**: < 5%

### Monitoring Dashboards
- Real-time booking success/failure rates
- Queue depth and processing time
- SEPE website availability
- User growth and engagement
- Error rates by category

---

## 9. Future Enhancements

### Short-term (3-6 months)
- Mobile app (React Native or Flutter)
- Multi-language support (Spanish, English, French)
- Browser extension for self-service mode
- Integration with other government services

### Long-term (6-12 months)
- AI-powered optimal time prediction
- Community-driven slot sharing alerts
- Automated document preparation
- Integration with job search platforms

---

## 10. Getting Started Checklist

### Prerequisites
- [ ] Java 17+ installed
- [ ] Node.js 18+ installed
- [ ] Docker & Docker Compose
- [ ] PostgreSQL client
- [ ] IDE (IntelliJ IDEA recommended)

### Initial Setup Commands
```bash
# Clone repository
git clone <repo-url>
cd sepe-cita-previa

# Backend setup
cd backend
./mvnw spring-boot:run

# Frontend setup
cd frontend
npm install
npm run dev

# Docker services
docker-compose up -d postgres redis rabbitmq
```

---

## Important Ethical Note

⚠️ **This application should be designed to:**
1. Help legitimate users access public services more efficiently
2. NOT overwhelm SEPE's infrastructure
3. Respect rate limits and server capacity
4. Be transparent about its operation
5. Comply with all applicable laws and regulations

The goal is to **assist users**, not to exploit or abuse the system. Consider implementing features that:
- Limit bookings per user
- Add reasonable delays between attempts
- Provide clear disclaimers about usage
- Cooperate with authorities if needed

---

## Next Steps

1. **Validate the concept** with potential users
2. **Consult legal counsel** regarding automation of government services
3. **Start with Phase 1** (basic infrastructure)
4. **Iterate quickly** based on feedback
5. **Monitor and adapt** to SEPE website changes

Good luck with your project! 🚀
