# AutoWizard - Vehicle Management System

AutoWizard is a Spring Boot-based backend application for managing multiple vehicles, their documents, and maintenance schedules.

## Features

### 1. User Authentication
- User registration and login with email/password
- OAuth2 integration with Google
- Password recovery functionality

### 2. Vehicle Management
- Add, modify, and remove vehicles
- Required fields:
  - Type
  - Brand
  - Model
  - License Plate
  - VIN
  - Mileage
  - Registration Year

### 3. Document Management
- Upload and manage vehicle documents (PDF/Photos)
- Supported document types:
  - Vehicle Registration
  - Insurance
  - Tax Documents
- Document viewing per vehicle

### 4. Reminders and Notifications
- Manual reminder creation
- Push notifications and email alerts (7 days before due date)
- Upcoming deadlines section in dashboard

### 5. Dashboard
- Vehicle status overview
- Updated mileage tracking
- Recent documents and notifications
- Traffic light system for approaching deadlines

### 6. AI Integration
- Basic AI query functionality (currently mocked)
- Endpoint: POST /api/ai/query

## Sistema di Notifiche e Reminder

Il sistema di notifiche è progettato per gestire i reminder relativi alle manutenzioni dei veicoli. I reminder sono associati agli utenti e possono contenere multiple task di manutenzione.

### Struttura dei Reminder

- **Associazione con Utenti**: Ogni reminder è associato a un utente specifico
- **Task Multiple**: Un reminder può contenere più task di manutenzione
- **Tipi di Reminder**:
  - `PENDING_TASKS`: Notifica per task pendenti
  - `OVERDUE_TASKS`: Notifica per task scadute
  - `MAINTENANCE_DUE`: Notifica per manutenzioni programmate

### Endpoint API

#### Creazione Reminder
```http
POST /api/reminders
Content-Type: application/json
Authorization: Bearer {token}

{
    "title": "Manutenzioni Pendenti",
    "message": "Hai 3 manutenzioni da eseguire",
    "notificationDate": "2024-03-20T10:00:00",
    "type": "PENDING_TASKS",
    "taskIds": [1, 2, 3]
}
```

#### Ottenere Reminder Pendenti
```http
GET /api/reminders/pending
Authorization: Bearer {token}
```

#### Ottenere Reminder Scaduti
```http
GET /api/reminders/overdue
Authorization: Bearer {token}
```

#### Marcare come Notificato
```http
PUT /api/reminders/{reminderId}/notify
Authorization: Bearer {token}
```

#### Generare Reminder di Manutenzione
```http
POST /api/reminders/generate
Authorization: Bearer {token}
```

### Sicurezza

- Tutti gli endpoint richiedono autenticazione JWT
- Gli utenti possono accedere solo ai propri reminder
- Le task incluse nei reminder devono appartenere a veicoli dell'utente

### Gestione degli Errori

Il sistema gestisce i seguenti casi di errore:
- Reminder non trovato
- Task non trovata
- Autorizzazione non valida
- Errori di validazione dei dati

## Technical Stack

- Java 17
- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA
- H2 Database (for development)
- JWT Authentication
- Spring Mail

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven
- IDE (IntelliJ IDEA recommended)

### Installation

1. Clone the repository
2. Configure application.properties:
   - Update email settings
   - Set JWT secret
   - Configure database settings if needed

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

### API Documentation

#### Authentication Endpoints
- POST /api/auth/register - Register new user
- POST /api/auth/login - User login
- POST /api/auth/forgot-password - Request password reset
- POST /api/auth/reset-password - Reset password

#### Vehicle Endpoints
- GET /api/vehicles - List all vehicles
- POST /api/vehicles - Add new vehicle
- PUT /api/vehicles/{id} - Update vehicle
- DELETE /api/vehicles/{id} - Remove vehicle
- GET /api/vehicles/{id} - Get vehicle details

#### Document Endpoints
- POST /api/documents/upload - Upload document
- GET /api/documents/vehicle/{vehicleId} - Get vehicle documents
- DELETE /api/documents/{id} - Delete document

#### Reminder Endpoints
- POST /api/reminders - Create reminder
- GET /api/reminders/upcoming - Get upcoming reminders
- PUT /api/reminders/{id} - Update reminder
- DELETE /api/reminders/{id} - Delete reminder

#### AI Endpoints
- POST /api/ai/query - Query AI service (currently mocked)

## Security

- JWT-based authentication
- Password encryption
- Role-based access control
- Secure file upload handling

## Development

### Database
The application uses H2 database in development mode. Access the H2 console at:
http://localhost:8080/h2-console

### Testing
Run tests with:
```bash
mvn test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License. 