# SWIFT Codes API

A Spring Boot application for managing SWIFT/BIC codes. This application parses SWIFT codes data from an Excel file, stores it in a MySQL database, and provides RESTful API endpoints for accessing and managing the data.

## Features

- Parses SWIFT codes from Excel file
- Identifies headquarters vs. branches (codes ending with "XXX" represent headquarters)
- Stores data in a relational database with optimized query performance
- Exposes RESTful API endpoints for data access and management
- Containerized for easy deployment

## API Endpoints

### 1. Get SWIFT Code Details

**GET /v1/swift-codes/{swift-code}**

Retrieves details of a specific SWIFT code. For headquarters, includes associated branches.

Example response for headquarters:
```json
{
  "swiftCode": "AAABBBCCXXX",
  "bankName": "EXAMPLE BANK",
  "address": "123 BANKING STREET",
  "countryISO2": "US",
  "countryName": "UNITED STATES",
  "isHeadquarter": true,
  "branches": [
    {
      "swiftCode": "AAABBBCC123",
      "bankName": "EXAMPLE BANK BRANCH",
      "address": "456 BRANCH AVENUE",
      "countryISO2": "US",
      "isHeadquarter": false
    }
  ]
}
```

### 2. Get SWIFT Codes by Country

**GET /v1/swift-codes/country/{countryISO2code}**

Returns all SWIFT codes for a specific country.

Example:
```json
{
  "countryISO2": "US",
  "countryName": "UNITED STATES",
  "swiftCodes": [
    {
      "swiftCode": "AAABBBCCXXX",
      "bankName": "EXAMPLE BANK",
      "address": "123 BANKING STREET",
      "countryISO2": "US",
      "isHeadquarter": true
    }
  ]
}
```

### 3. Add New SWIFT Code

**POST /v1/swift-codes**

Adds a new SWIFT code to the database.

Request body:
```json
{
  "swiftCode": "NEWTESTZXXX",
  "bankName": "TEST INTERNATIONAL BANK",
  "address": "123 Finance Street",
  "countryISO2": "GB",
  "countryName": "UNITED KINGDOM",
  "isHeadquarter": true
}
```

### 4. Delete SWIFT Code

**DELETE /v1/swift-codes/{swift-code}**

Deletes a SWIFT code from the database.

## Setup and Installation

### Prerequisites
- Java 17 or later
- Maven
- MySQL 8.0 or later
- Docker (optional, for containerized deployment)

### Database Configuration
Update the database configuration in `src/main/resources/application.properties`:
```
spring.datasource.url=jdbc:mysql://localhost:3306/swift_codes_db?createDatabaseIfNotExist=true
spring.datasource.username=your-username
spring.datasource.password=your-password
```

### Running the Application
1. Clone the repository
2. Build the application:
   ```
   mvn clean package
   ```
3. Run the application:
   ```
   mvn spring-boot:run
   ```
4. Import data (place Excel file in the project root):
   ```
   curl http://localhost:8080/import-data
   ```

### Running with Docker
1. Build the Docker image:
   ```
   docker build -t swift-codes-api .
   ```
2. Run the container:
   ```
   docker run -p 8080:8080 swift-codes-api
   ```

Or use Docker Compose:
```
docker-compose up -d
```

## Project Structure
- `SwiftCodeProcessor`: Parses Excel file and stores data in database
- `SwiftCodeController`: REST API endpoints
- `SwiftCode`: Data model for SWIFT codes

## Technologies
- Spring Boot
- Apache POI (for Excel parsing)
- MySQL
- Docker
