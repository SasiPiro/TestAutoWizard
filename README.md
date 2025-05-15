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