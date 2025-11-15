# URL Shortener Service

A scalable, monitored URL shortener service built with Spring Boot. This service supports short URL generation, click tracking, QR code generation, blacklisting, and automated cleanup of expired URLs. It leverages Redis for caching, Kafka for event streaming, and Micrometer for metrics.

---

## Table of Contents

* [Features](#features)
* [Architecture](#architecture)
* [Technologies](#technologies)
* [Configuration](#configuration)
* [API Endpoints](#api-endpoints)
* [Usage](#usage)
* [Docker Quick Start](#docker-quick-start)
* [Metrics & Monitoring](#metrics--monitoring)
* [Known Limitations](#known-limitations)

---

## Features

* Generate short URLs with optional custom aliases.
* Click tracking with Kafka event streaming and batch counting in Redis.
* QR code generation and caching in Redis.
* URL blacklisting and abuse detection.
* Automatic cleanup:

    * Deactivate expired URLs hourly.
    * Permanently delete soft-deleted URLs daily.
* Caching in Redis for fast URL resolution.
* Swagger/OpenAPI documentation.
* Metrics for expired URLs, deleted URLs, and cleanup duration via Micrometer.

---

## Architecture

1. **Web Layer**: Handles API requests for URL creation, resolution, and click tracking.
2. **Service Layer**:

    * `UrlService`: Central service for URL creation, caching, and click event publishing.
    * `QrCodeService`: Generates and caches QR codes for short URLs.
    * `BlacklistService`: Checks URLs against blacklisted patterns.
    * `AbuseEventService`: Records abuse events for monitoring.
3. **Persistence Layer**:

    * `UrlRepository`: Stores URL entities in PostgreSQL.
    * `RedisTemplate`: Caches URL and QR code data for fast retrieval.
4. **Messaging**:

    * Kafka topic `clicks` for click event publishing and consumption.
    * `ClickBatchConsumer` updates click counts in Redis.
5. **Workers / Scheduling**:

    * `MonitoredUrlCleanupWorker` handles deactivation and deletion of URLs.
6. **Metrics**:

    * Exposes counters and timers for monitoring URL cleanup operations.

---

## Technologies

* **Backend**: Java, Spring Boot, Spring Data JPA
* **Database**: PostgreSQL
* **Cache**: Redis
* **Messaging**: Kafka
* **Metrics**: Micrometer (Prometheus-compatible)
* **QR Code**: ZXing
* **API Docs**: OpenAPI / Swagger
* **Logging**: SLF4J / Logback

---

## Configuration

* **Async Execution**: Configured `ThreadPoolTaskExecutor` for background tasks.
* **Kafka**: `clicks` topic with 6 partitions and replication factor 3.
* **Redis**:

    * `RedisTemplate<String, Object>` for JSON object caching.
    * `StringRedisTemplate` for simple string operations.
* **Scheduler**: Configured a thread pool for `@Scheduled` tasks.
* **QR Code**: Width, height, and TTL configurable via application properties.

---

## API Endpoints

### Create Short URL

```
POST /api/urls
Body:
{
  "originalUrl": "https://example.com",
  "customAlias": "my-link",      // optional
  "expiresAt": "2025-12-31T23:59:59Z"  // optional
}
```

### Resolve Short URL

```
GET /{shortKey}
```

### Get QR Code

```
GET /{shortKey}/qr
```

### Blacklist Management

```
POST /api/blacklist
Body:
{
  "pattern": "malicious.com"
}
```

---

## Usage

1. Configure application properties for DB, Redis, Kafka, and QR code settings.
2. Run the Spring Boot application:

```bash
./mvnw spring-boot:run
```

3. Short URLs are automatically cached and click events are streamed to Kafka.

---

## Docker Quick Start

```bash
# Start all dependencies
docker-compose up -d redis postgres kafka

# Run the application
mvn spring-boot:run
```

## API Documentation

* **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Metrics & Monitoring

* **Counters**:

    * `url.cleanup.expired` → Number of expired URLs deactivated.
    * `url.cleanup.permanently_deleted` → Number of URLs permanently deleted.
* **Timer**:

    * `url.cleanup.duration` → Time spent on cleanup operations.
* Metrics can be scraped by Prometheus and visualized in Grafana.

---

## Known Limitations

* Race conditions possible during automatic short key generation under high concurrency.
* QR code generation is synchronous; high load may cause CPU spikes.
* Redis cache may become stale if TTL not properly synchronized with DB expiry.
* Kafka failures may result in lost click events; no retry mechanism implemented.
* Blacklist search scans all entries in DB → may be slow if blacklist is large.

---

## Future Improvements

* Add async QR code generation with pre-caching.
* Implement retry or dead-letter queue for Kafka event publishing.
* Optimize blacklist lookup using in-memory structures like Trie or Bloom filter.
* Use database-level locking or unique key generation strategies to prevent race conditions.
* Add rate-limiting and abuse protection per IP.
