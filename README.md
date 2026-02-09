# Web Application

Cloud-native web application with RESTful APIs for user management and health checks.

## Prerequisites

### Required Software
- **Java Development Kit (JDK)**: 17 or higher
- **Maven**: 3.6 or higher
- **MySQL**: 8.0 or higher
- **Git**: Latest version

### Optional Tools
- Postman or cURL for API testing
- IDE (IntelliJ IDEA, VS Code, or Eclipse)

## Project Setup

### 1. Clone the Repository

**Clone your fork:**
```bash
git clone git@github.com:DBAA21/webapp.git
cd webapp
```

**Add upstream remote:**
```bash
git remote add upstream git@github.com:DBAAcsye6225/webapp.git
```

### 2. Database Configuration

**Create MySQL database:**
```bash
mysql -u root -p
CREATE DATABASE csye6225;
EXIT;
```

**Note:** The application uses Hibernate's automatic schema management (`spring.jpa.hibernate.ddl-auto=update`). Database tables, indexes, and constraints will be created automatically on first run. No manual SQL scripts are required.

### 3. Configure Application

The application uses environment variables for database configuration, following cloud-native best practices.

**Option A: Environment Variables (Recommended)**

Set the following environment variables before running the application:
```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/csye6225
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
```

**Option B: Update Configuration File (Development Only)**

For local development, you can modify `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/csye6225?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=your_username
spring.datasource.password=your_password
```

**⚠️ Security Warning:** Never commit passwords or sensitive credentials to Git. Always use environment variables for production deployments.

## Build Instructions

### Build the Application
```bash
# Clean and build
mvn clean install

# Skip tests during build
mvn clean install -DskipTests
```

### Run Tests
```bash
# Run all integration tests
mvn test

# Run with custom database configuration
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/test_db \
SPRING_DATASOURCE_USERNAME=test_user \
SPRING_DATASOURCE_PASSWORD=test_password \
mvn test
```

## Deploy Instructions

### Run Locally

**Using Maven:**
```bash
mvn spring-boot:run
```

**Using JAR file:**
```bash
# Build the JAR
mvn clean package

# Run the JAR
java -jar target/webapp-0.0.1-SNAPSHOT.jar
```

**With environment variables:**
```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/csye6225 \
SPRING_DATASOURCE_USERNAME=your_username \
SPRING_DATASOURCE_PASSWORD=your_password \
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

Full API specifications: [Swagger Documentation](https://app.swaggerhub.com/apis-docs/csye6225-webapp/cloud-native-webapp/spring2026-a1)

### Available Endpoints

#### Health Check
- `GET /healthz` - Health check endpoint
  - Returns `200 OK` when service is healthy
  - No authentication required
  - No request body or query parameters allowed

#### User Management
- `POST /v1/user` - Create a new user account
  - No authentication required
  - Returns `201 Created` with user details
- `GET /v1/user/self` - Get current authenticated user details
  - Requires HTTP Basic Authentication
  - Returns `200 OK` with user information
- `PUT /v1/user/self` - Update authenticated user information
  - Requires HTTP Basic Authentication
  - Returns `204 No Content` on success

### Example API Calls

**Health Check:**
```bash
curl -X GET http://localhost:8080/healthz
```

**Create User:**
```bash
curl -X POST http://localhost:8080/v1/user \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane.doe@example.com",
    "password": "SecureP@ssw0rd",
    "first_name": "Jane",
    "last_name": "Doe"
  }'
```

**Get User Info (with Basic Authentication):**
```bash
curl -X GET http://localhost:8080/v1/user/self \
  -u "jane.doe@example.com:SecureP@ssw0rd"
```

**Update User:**
```bash
curl -X PUT http://localhost:8080/v1/user/self \
  -u "jane.doe@example.com:SecureP@ssw0rd" \
  -H "Content-Type: application/json" \
  -d '{
    "first_name": "Janet",
    "last_name": "Smith",
    "password": "NewP@ssw0rd123"
  }'
```

## Project Structure
```
webapp/
├── .github/
│   └── workflows/
│       └── ci.yml               # GitHub Actions CI/CD pipeline
├── src/
│   ├── main/
│   │   ├── java/com/csye6225/webapp/
│   │   │   ├── config/          # Security and application configuration
│   │   │   ├── controller/      # REST API controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities (User, HealthCheck)
│   │   │   ├── exception/       # Global exception handlers
│   │   │   ├── repository/      # Database repositories
│   │   │   ├── service/         # Business logic services
│   │   │   └── WebappApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       ├── java/                # Integration tests (26 test cases)
│       └── resources/
│           └── application-test.properties  # Test-specific configuration
├── .gitignore
├── pom.xml
└── README.md
```

## Testing

### Integration Test Suite

The project includes **26 comprehensive integration tests** covering:

**Health Check API (5 tests)**
- Successful health check
- Bad request scenarios (query parameters, request body)
- Method not allowed tests (POST, PUT, DELETE)

**User Creation API (6 tests)**
- Successful user creation
- Invalid email format validation
- Missing required fields validation
- Weak password validation
- Duplicate email conflict handling
- Wrong content-type handling

**User Retrieval API (3 tests)**
- Successful authenticated user retrieval
- Missing authentication credentials
- Non-existent user handling

**User Update API (12 tests)**
- Successful user updates (full and partial)
- Password update and verification
- Read-only field protection (id, username, account_created, account_updated)
- Authentication requirement validation
- Wrong content-type handling

### Run Tests Locally
```bash
# Run all tests
mvn test

# Run with test database
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/webapp_test \
SPRING_DATASOURCE_USERNAME=test \
SPRING_DATASOURCE_PASSWORD=test \
SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop \
mvn test

# Run specific test class
mvn test -Dtest=WebappApplicationTests

# Run with verbose output
mvn test -X
```

## CI/CD Pipeline

This project uses **GitHub Actions** for continuous integration and deployment.

### Automated Workflow

On every pull request to `main`:
1. ✅ Code checkout
2. ✅ Java 17 environment setup
3. ✅ MySQL 8.0 service initialization
4. ✅ Dependency installation
5. ✅ Integration test execution (all 26 tests)
6. ✅ Test report generation

### Branch Protection

The `main` branch is protected with the following rules:
- ✅ Pull requests required before merging
- ✅ Status checks must pass (CI tests)
- ✅ Branches must be up to date
- ❌ Force pushes disabled
- ❌ Branch deletion disabled

**See `.github/workflows/ci.yml` for complete workflow configuration.**

## Contributing

### Development Workflow

1. **Create a feature branch:**
```bash
   git checkout -b feature-name
```

2. **Make your changes and test locally:**
```bash
   mvn test
```

3. **Commit your changes:**
```bash
   git add .
   git commit -m "feat: description of your changes"
```

4. **Push to your fork:**
```bash
   git push origin feature-name
```

5. **Create a Pull Request:**
   - Navigate to the organization repository
   - Click "New pull request"
   - Select your fork and branch
   - Wait for CI checks to pass
   - Request review from TAs

### Commit Message Convention

Follow conventional commits format:
- `feat:` New features
- `fix:` Bug fixes
- `docs:` Documentation changes
- `test:` Test additions or modifications
- `refactor:` Code refactoring
- `ci:` CI/CD configuration changes

## Technology Stack

### Backend
- **Framework:** Spring Boot 3.5.10
- **Language:** Java 17
- **Build Tool:** Maven 3.9+

### Database
- **Database:** MySQL 8.0
- **ORM:** Hibernate (JPA)
- **Schema Management:** Automatic (ddl-auto)

### Security
- **Authentication:** HTTP Basic Authentication
- **Password Hashing:** BCrypt

### Testing
- **Framework:** JUnit 5
- **Integration Testing:** Spring Boot Test, MockMvc
- **Test Coverage:** 26 comprehensive test cases

### CI/CD
- **Platform:** GitHub Actions
- **Container:** MySQL Docker service
- **Environment:** Ubuntu Latest

## Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SPRING_DATASOURCE_URL` | JDBC connection URL | No | `jdbc:mysql://localhost:3306/csye6225` |
| `SPRING_DATASOURCE_USERNAME` | Database username | No | `DBAA` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | No | (local config) |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Schema management mode | No | `update` |

## Troubleshooting

### Common Issues

**Database Connection Failed:**
```bash
# Check MySQL is running
sudo systemctl status mysql

# Verify database exists
mysql -u root -p -e "SHOW DATABASES;"
```

**Tests Failing Locally:**
```bash
# Clean build and run tests
mvn clean test

# Check database configuration
cat src/main/resources/application.properties
```

**Port 8080 Already in Use:**
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

## Authors

- **Weihong Lian** - Initial development and implementation

## License

This project is developed for educational purposes as part of the **CSYE6225 - Network Structures and Cloud Computing** course at **Northeastern University**.

## Acknowledgments

- Course instructors and TAs for guidance and requirements
- Spring Boot documentation and community
- GitHub Actions for CI/CD platform

-test A02 Demo