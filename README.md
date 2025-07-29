# Currency Exchange Rate Service

A Spring Boot application that provides REST APIs for managing currencies and their exchange rates, with integration to the Open Exchange Rates API.

## Features

- Add new currencies
- Get a list of all supported currencies
- Get current exchange rates between any two supported currencies
- Automatic hourly updates of exchange rates
- In-memory caching for fast access to frequently requested rates
- OpenAPI documentation

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker and Docker Compose
- Open Exchange Rates API key (free tier available at https://openexchangerates.org/)

## Getting Started

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd currency
   ```

2. **Set up environment variables**
   Create a file named `.env` in the project root with the following content:
   ```
   OPENEXCHANGERATES_API_KEY=your_api_key_here
   ```
   Replace `your_api_key_here` with your actual Open Exchange Rates API key.

3. **Start the database**
   ```bash
   docker-compose up -d
   ```
   This will start a PostgreSQL database in a Docker container.

4. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```
   The application will be available at `http://localhost:8080`.

## API Documentation

Once the application is running, you can access the OpenAPI documentation at:

- Swagger UI: http://localhost:8080/swagger-ui.html

## API Limitations

### Open Exchange Rates Free Plan Restrictions

When using the free tier of Open Exchange Rates API, please be aware of the following limitations:

1. **Base Currency Restriction**:
   - Only USD can be used as the base currency for exchange rate requests
   - Attempting to use any other currency as the base will result in a 403 Forbidden error
   - This is a limitation of the free plan and not an issue with this application

2. **Rate Limits**:
   - Limited number of API calls per month (varies by plan)
   - Free plan has hourly updates (not real-time)

3. **Upgrading**:
   - To remove these limitations, consider upgrading to a paid plan at [Open Exchange Rates](https://openexchangerates.org/signup)
   - Paid plans offer additional base currencies, more frequent updates, and higher API call limits


## Available Endpoints

### Currencies
- `GET /api/v1/currencies` - Get all currencies
- `POST /api/v1/currencies` - Add a new currency

### Exchange Rates
- `GET /api/v1/exchange-rates?base=USD&target=EUR` - Get exchange rate between two currencies

## Database

The application uses PostgreSQL with the following schema:
- `currency` - Stores currency information (code, name)
- `exchange_rate` - Stores historical exchange rates

Database migrations are managed by Flyway and are located in `src/main/resources/db/migration/`.

## Configuration

Application properties can be configured in `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=admin
spring.datasource.password=your_password

# Open Exchange Rates API
openexchangerates.api-key=${OPENEXCHANGERATES_API_KEY}
openexchangerates.base-url=https://openexchangerates.org/api/

# Scheduling (in milliseconds)
app.scheduling.fixed-rate=3600000  # 1 hour
```

## Testing

Run the tests with:
```bash
mvn test
```
