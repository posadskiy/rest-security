# rest-security

Authorization without the authentication overhead. Secure your application methods by **role** and **same-user access** — with or without Spring, with or without JWT.

- **Java 21+**
- **Zero-dependency core** (`rest-security-core`): drop-in authorization checks in any Java app
- **JWT adapter** (`rest-security-jwt`): treat JWT as session + user + roles
- **Spring integration** (`rest-security-spring`): `@Security` / `@Public` for bean methods

When deciding between frameworks, see [Spring Security vs rest-security](docs/COMPARISON.md).

---

## Table of contents

- [Choose a module](#choose-a-module)
- [Install](#install)
- [Concepts](#concepts)
- [Quick start: Plain Java](#quick-start-plain-java)
- [Quick start: JWT](#quick-start-jwt)
- [Quick start: Spring](#quick-start-spring)
- [Audit logging](#audit-logging)
- [Security notes](#security-notes)
- [Troubleshooting](#troubleshooting)
- [Examples](#examples)
- [Build & test](#build--test)
- [Test coverage](#test-coverage)
- [Requirements](#requirements)
- [License](#license)

---

## Choose a module

| You want… | Add dependency | What you get |
|-----------|----------------|--------------|
| **Plain Java, no framework** | `rest-security-core` | `SecurityEnforcer`, `SecuredRequest`, `SecurityContextHolder`, annotations, exceptions. **Zero dependencies**. |
| **JWT where token = session + user + roles** | `rest-security-core` + `rest-security-jwt` | `JwtSecurityController` implements both session + user contracts from JWT claims. |
| **Spring + annotations** | `rest-security-spring` | `SecurityAnnotationBeanPostProcessor`, `@Security`, `@Public`. Bring your own controllers or use JWT. |

Notes:
- `rest-security-spring` depends on `rest-security-core`
- `rest-security-jwt` depends on `rest-security-core`

---

## Install

### Maven

```xml
<properties>
    <rest-security.version>1.0.0</rest-security.version>
</properties>

<!-- Core only -->
<dependency>
    <groupId>com.posadskiy</groupId>
    <artifactId>rest-security-core</artifactId>
    <version>${rest-security.version}</version>
</dependency>

<!-- JWT adapter (optional) -->
<dependency>
    <groupId>com.posadskiy</groupId>
    <artifactId>rest-security-jwt</artifactId>
    <version>${rest-security.version}</version>
</dependency>

<!-- Spring integration (optional) -->
<dependency>
    <groupId>com.posadskiy</groupId>
    <artifactId>rest-security-spring</artifactId>
    <version>${rest-security.version}</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.posadskiy:rest-security-core:1.0.0")
    // implementation("com.posadskiy:rest-security-jwt:1.0.0")
    // implementation("com.posadskiy:rest-security-spring:1.0.0")
}
```

---

## Concepts

- **Session**: an identifier used to authenticate the caller. With JWT, the “session id” is the token itself.
- **User**: resolved from session. Must exist and have roles.
- **Roles**: caller must have at least one required role (unless ADMIN).
- **Same-user access**: if request contains a *target user id*, non-admin callers can only access their own user.
- **SecurityContext**: resolved `sessionId`, `userId`, `roles` and stored in **`SecurityContextHolder`** for the duration of a secured call.

You provide two small adapters:
- `SessionSecurityController` — “does this session exist / expired?” + “resolve user id”
- `UserSecurityController` — “does this user exist?” + “resolve roles”

---

## Quick start: Plain Java

### 1) Provide controllers

Implement these interfaces (DB, cache, your own auth service — anything):

- `SessionSecurityController`
- `UserSecurityController`

### 2) Enforce authorization

```java
var enforcer = new SecurityEnforcer(sessionController, userController);
SecuredRequestContext request = new SecuredRequest(sessionId);

// Sets SecurityContextHolder on success
SecurityContext ctx = enforcer.enforce(request, "USER");
try {
    return doWork(ctx.userId());
} finally {
    SecurityContextHolder.clearContext();
}
```

Prefer the “auto-clear” helpers in request handlers:

```java
return enforcer.enforceAndCall(request, new String[]{"USER"}, () -> {
    return doWork(SecurityContextHolder.getContext().userId());
});
```

### Same-user access

If you pass `userId` in the request, non-admins can only access themselves:

```java
SecuredRequestContext request = new SecuredRequest(sessionId, targetUserId, null);
enforcer.enforce(request, "USER");
```

---

## Quick start: JWT

JWT mode requires **no session store** and **no user DB** — user and roles come from JWT claims.

```java
JwtConfig config = JwtConfig.withSecret("your-hmac-secret");
JwtSecurityController jwt = new JwtSecurityController(config);
SecurityEnforcer enforcer = new SecurityEnforcer(jwt, jwt);

// token from Authorization header
SecuredRequestContext request = new SecuredRequest(token);
enforcer.enforce(request, "USER");
```

Custom claim names are supported:

```java
JwtConfig config = new JwtConfig("your-hmac-secret", "authorities", "uid", false);
```

---

## Quick start: Spring

### 1) Register the processor

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityAnnotationBeanPostProcessor securityProcessor(
            SessionSecurityController sessionController,
            UserSecurityController userController) {
        return new SecurityAnnotationBeanPostProcessor(sessionController, userController);
    }
}
```

If you use JWT, you can wire a single adapter into both:

```java
@Bean
public JwtSecurityController jwtSecurityController() {
    return new JwtSecurityController(JwtConfig.withSecret("your-hmac-secret"));
}

@Bean
public SecurityAnnotationBeanPostProcessor securityProcessor(JwtSecurityController jwt) {
    return new SecurityAnnotationBeanPostProcessor(jwt, jwt);
}
```

### 2) Annotate an interface method

**Important**: Spring integration uses JDK dynamic proxies, so your secured bean should implement an interface and you should call it via that interface type.

```java
public interface UserApi {
    @Security(roles = {"USER"})
    UserProfile getProfile(SecuredRequestContext request);

    @Public
    Health health(SecuredRequestContext request);
}

@Service
public class UserApiImpl implements UserApi {
    @Override
    public UserProfile getProfile(SecuredRequestContext request) {
        SecurityContext ctx = SecurityContextHolder.getContext();
        return loadProfile(ctx.userId());
    }
}
```

### 3) Provide the request context as the first argument

```java
userApi.getProfile(new SecuredRequest(sessionId));
```

---

## Audit logging

### Plain Java

```java
enforcer.setAuditListener(new SecurityAuditListener() {
    @Override
    public void onAuthenticationSuccess(SecurityContext context, String method) {
        log.info("SEC OK {} {}", method, context.userId());
    }

    @Override
    public void onAuthenticationFailure(String sessionId, String method, RestSecurityException exception) {
        log.warn("SEC FAIL {} {} {}", method, sessionId, exception.getClass().getSimpleName());
    }
});
```

If you want a custom method name in audit logs for `enforce()`, use:
- `SecurityEnforcer.enforceWithMethodName(ctx, methodName, requiredRoles)`

### Spring

Register `SecurityAuditListener` as a Spring bean — it will be auto-wired into the processor.

---

## Security notes

- **`trustGateway=true` disables signature verification.** Use it only when a trusted gateway (or sidecar) already verified the JWT and you can guarantee tokens are not user-controlled.
- Always clear `SecurityContextHolder` after a request. Use `enforceAndRun` / `enforceAndCall` where possible.
- `ADMIN` role bypasses role checks and same-user restriction.

---

## Troubleshooting

- **My secured Spring bean is not proxied**
  - Ensure the bean **implements an interface** and you call it through that interface.
  - Beans without interfaces are returned “as-is” (no proxy).
- **I get `IllegalArgumentException: @Security method ... first parameter must be SecuredRequestContext`**
  - Make sure the **first argument** is `SecuredRequestContext` (e.g. `new SecuredRequest(sessionId)`).
- **JWT always returns “session does not exist”**
  - If `trustGateway=false`, your `secret` must be non-blank and must match the token signature algorithm (HMAC).

---

## Examples

Runnable example projects are in **[examples/](examples/)**. Each demonstrates a different setup:

| Example | Description |
|---------|-------------|
| [01-plain-java-inmemory](examples/01-plain-java-inmemory/) | Plain Java + Javalin; in-memory session and user stores; `SecurityEnforcer` only. |
| [02-jwt-lightweight-http](examples/02-jwt-lightweight-http/) | JWT with a lightweight HTTP server; no session store or user DB. |
| [03-spring-boot-jwt](examples/03-spring-boot-jwt/) | Spring Boot REST API with JWT and `@Security` / `@Public`. |
| [04-spring-boot-custom-store](examples/04-spring-boot-custom-store/) | Spring Boot with custom session and user controllers (e.g. Redis + DB). |
| [05-gateway-microservice](examples/05-gateway-microservice/) | Gateway signs JWTs; microservice uses `trustGateway=true`. |

See **[examples/README.md](examples/README.md)** for a comparison table, run commands, and which example to pick for your scenario.

---

## Build & test

```bash
mvn clean test
```

To run tests and enforce coverage thresholds (see below):

```bash
mvn clean verify
```

---

## Test coverage

Test coverage is measured with [JaCoCo](https://www.jacoco.org/jacoco/). Minimum line coverage is enforced per module; the build fails if coverage drops below the threshold.

| Module | Minimum line coverage |
|--------|------------------------|
| rest-security-core | 90% |
| rest-security-jwt | 90% |
| rest-security-spring | 80% |

### What you can do

| Goal | Command | Description |
|------|---------|-------------|
| Run tests | `mvn test` | Runs all tests. Coverage data is collected and HTML reports are generated per module. |
| Enforce coverage | `mvn verify` | Runs tests and then **fails the build** if any module is below its minimum line coverage (see table above). |
| View report | Open `*/target/site/jacoco/index.html` | After `mvn test`, open the report in a browser (e.g. `rest-security-core/target/site/jacoco/index.html`). |

### Where reports are generated

- **rest-security-core**: `rest-security-core/target/site/jacoco/index.html`
- **rest-security-jwt**: `rest-security-jwt/target/site/jacoco/index.html`
- **rest-security-spring**: `rest-security-spring/target/site/jacoco/index.html`

### Changing the coverage threshold

In each module’s `pom.xml`, the JaCoCo plugin has a `check` execution with a `<minimum>` value (e.g. `0.90` for 90%). Adjust it to relax or tighten the requirement.

---

## Requirements

- Java 21+
- Spring (only if using `rest-security-spring`): Spring Context 6.x

---

## License

MIT
