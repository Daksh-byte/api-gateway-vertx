# API Gateway with Vert.x

A lightweight API Gateway built with Eclipse Vert.x that aggregates data from multiple external APIs with circuit breaker pattern for resilience.

Approach :
Built using Vert.x to create a lightweight API Gateway.

Created an HTTP server with endpoints /aggregate and /health.

Used WebClient to call two external APIs (posts & users).

Executed both API calls asynchronously in parallel using CompositeFuture.

Combined the responses (post title + author name) into a single JSON output.

Returned proper error responses when external calls failed.

Project kept simple, clean, and non-blocking to maintain high performance.

## Features

- Data Aggregation: Combines data from JSONPlaceholder APIs (posts and users)
- Circuit Breaker: Fault tolerance with automatic failure detection and recovery
- Health Check: Built-in health monitoring endpoint
- Async Processing: Non-blocking I/O with Vert.x reactive architecture
- Comprehensive Testing: JUnit 5 integration tests

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Vert.x Core & Web Dependencies

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd api-gateway-vertx
mvn clean compile
```

### 2. Run the Application

```bash
# Using Maven
mvn exec:java

# Or run the fat JAR
mvn package
java -jar target/vertx-api-gateway-1.0.0-fat.jar
```

### 3. Test the Endpoints

```bash
# Health check
curl http://localhost:8080/health

# Data aggregation
curl http://localhost:8080/aggregate
```

## API Endpoints

### GET /health
Returns the service health status.

**Response:**
```json
{
  "status": "UP",
  "timestamp": 1703123456789,
  "service": "API Gateway"
}
```

### GET /aggregate
Aggregates data from external APIs (post title and author name).

**Response:**
```json
{
  "post_title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
  "author_name": "Leanne Graham"
}
```

**Error Response:**
```json
{
  "error": "Failed to fetch data from external APIs",
  "message": "Connection timeout",
  "timestamp": 1703123456789
}
```

## Architecture

- **ApiGatewayVerticle**: Main application verticle handling HTTP requests
- **MainVerticle**: Bootstrap verticle for deployment orchestration
- **Circuit Breaker**: Protects against cascading failures from external APIs
- **WebClient**: Async HTTP client for external API calls

## Configuration

The application uses the following default settings:

- **Port**: 8080
- **Connection Timeout**: 5 seconds
- **Circuit Breaker**: 3 max failures, 10 second reset timeout
- **External APIs**: JSONPlaceholder (posts and users)

## Testing

Run the test suite:

```bash
mvn test
```

The tests cover:
- Health endpoint functionality
- Aggregate endpoint structure and values
- Error handling for undefined routes
- Response validation

## Development

### Project Structure

```
src/
├── main/java/com/gateway/
│   ├── ApiGatewayVerticle.java    # Main application logic
│   └── MainVerticle.java          # Bootstrap verticle
└── test/java/com/gateway/
    └── ApiGatewayTest.java        # Integration tests
```

### Key Dependencies

- **Vert.x Core**: Reactive toolkit
- **Vert.x Web**: HTTP server and routing
- **Vert.x Web Client**: Async HTTP client
- **Vert.x Circuit Breaker**: Fault tolerance
- **JUnit 5**: Testing framework

## Monitoring

The application provides console logging for:
- Server startup/shutdown events
- Request processing
- External API calls
- Circuit breaker state changes
- Error conditions
