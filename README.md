# Spring Boot URL Shortener (High Capacity)

This is a runnable starter project for a high-capacity URL shortener using Spring Boot, PostgreSQL, Redis, and Kafka (skeleton).

Run locally (requires Postgres, Redis, Kafka):
- Configure `application.yml` with connection strings.
- `mvn -U -DskipTests package` then `java -jar target/shortener-0.0.1-SNAPSHOT.jar`

This zip contains source, Dockerfile, Flyway migration and simple k8s manifests.


todo:
~~Batch Click Processing~~
~~Database Sharding / Partitioning~~
~~Blacklist / Safe URLs~~
Abuse Detection

Clicks حسب البلد / الجهاز / الـ browser

Authentication & Authorization

Prometheus
Grafana
Elasticsearch + Kibana
Kafka Connect
Nginx



احفظ في Redis أو في الذاكرة (cache) آخر created_at لكل shortKey.
لأنك تحتاج هذا التاريخ لتحديد الـ partition لاحقًا بسرعة.