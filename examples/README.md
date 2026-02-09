# rest-security examples

Runnable example projects that show how to use [rest-security](../README.md) in different setups. Each example is self-contained and can be run with a single command.

**Prerequisite:** Build and install the rest-security library so examples can resolve the dependency:

```bash
cd ..   # project root
mvn clean install -DskipTests
```

Then run any example from its directory (e.g. `cd 01-plain-java-inmemory && mvn exec:java`).

---

## Which example should I run?

| Example | Use case | Modules used | Run command |
|---------|----------|--------------|-------------|
| [01-plain-java-inmemory](01-plain-java-inmemory/) | Plain Java, no framework | `rest-security-core` | `mvn exec:java` |
| [02-jwt-lightweight-http](02-jwt-lightweight-http/) | JWT, no Spring | `rest-security-core` + `rest-security-jwt` | `mvn exec:java` |
| [03-spring-boot-jwt](03-spring-boot-jwt/) | Spring Boot + JWT | All three modules | `mvn spring-boot:run` |
| [04-spring-boot-custom-store](04-spring-boot-custom-store/) | Spring Boot + custom auth | `rest-security-core` + `rest-security-spring` | `mvn spring-boot:run` |
| [05-gateway-microservice](05-gateway-microservice/) | Microservices + gateway | `rest-security-jwt` (trustGateway) | `docker-compose up` |

---

## By scenario

- **I'm not using Spring** → Start with [01-plain-java-inmemory](01-plain-java-inmemory/) or [02-jwt-lightweight-http](02-jwt-lightweight-http/) (JWT only).
- **I use Spring Boot and JWT** → [03-spring-boot-jwt](03-spring-boot-jwt/).
- **I have my own session/user store (Redis, DB)** → [04-spring-boot-custom-store](04-spring-boot-custom-store/).
- **My service sits behind a gateway that already validates tokens** → [05-gateway-microservice](05-gateway-microservice/).

---

## Concepts

All examples rely on the same ideas: **session** (or JWT) identifies the caller, **user** and **roles** are resolved from it, and rest-security enforces **role checks** and **same-user access**. See the [main README](../README.md) for concepts and API details.

---

**Note:** Examples use in-memory or mock data only. They are for learning and local runs, not production deployment.
