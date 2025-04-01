# SWIFT Codes API
## Features

- Parses SWIFT codes from Excel file
- Identifies headquarters vs. branches (codes ending with "XXX" represent headquarters)
- Stores data in a relational database with optimized query performance
- Exposes RESTful API endpoints for data access and management
- Containerized for easy deployment

## Quick Start with Docker

The easiest way to run this application is using Docker. This approach requires no local Java, Maven, or MySQL installation.

### Prerequisites
- Docker and Docker Compose
- Excel file with SWIFT codes (named `Interns_2025_SWIFT_CODES.xlsx`)

### Running the Application

1. Clone the repository
2. Place your `Interns_2025_SWIFT_CODES.xlsx` file in the project root directory
3. Run using Docker Compose:
   ```
   docker-compose up
   ```
4. Import data by accessing:
   ```
   http://localhost:8080/import-data
   ```
5. The application is now running at `http://localhost:8080`

To stop the application:
```
docker-compose down
```

## API Endpoints

### 0. Import Data (Utility Endpoint)

**GET /import-data**

Triggers the import process to parse SWIFT codes from the Excel file and store them in the database.

Example:
```
GET http://localhost:8080/import-data
```

Response:
```json
"Successfully imported 1250 SWIFT codes"
```

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

## Docker Compose Configuration

The application uses Docker Compose to set up both the API service and the MySQL database. Key configuration settings:

- MySQL database configured with an empty root password
- Database name: `swift_codes_db`
- API server accessible on port 8080
- Excel file mounted as a volume to make it accessible to the container


## Project Structure
- `SwiftCodeProcessor`: Parses Excel file and stores data in database
- `SwiftCodeController`: REST API endpoints
- `SwiftCode`: Data model for SWIFT codes

## Technologies
- Spring Boot
- Apache POI (for Excel parsing)
- MySQL
- Docker
