# Example 04: Spring Boot with custom session/user store

## What this demonstrates

- Integrating rest-security with **existing auth infrastructure**: your session store and user store.
- Custom `SessionSecurityController` (Redis-like with `ConcurrentHashMap`) and custom `UserSecurityController` (in-memory "database").
- Login creates a session in the Redis-like store; protected endpoints resolve user and roles from it.
- **Same-user enforcement**: `OrderController` uses path variable `userId`; non-admins can only see their own orders (enforcer checks `SecuredRequest(sessionId, targetUserId, null)`).

## How to run

From the project root, install the library, then:

```bash
cd examples/04-spring-boot-custom-store
mvn spring-boot:run
```

Server starts at http://localhost:8080.

## Try it

```bash
# Login (session stored in "Redis")
curl -s -X POST http://localhost:8080/api/login -H "Content-Type: application/json" -d '{"username":"alice","password":"x"}' -c cookies.txt

# My orders (session from cookie)
curl -s http://localhost:8080/api/orders -b cookies.txt

# Orders for a user (same-user: alice can only request /api/orders/alice)
curl -s http://localhost:8080/api/orders/alice -b cookies.txt

# As admin, can request another user's orders
curl -s -X POST http://localhost:8080/api/login -H "Content-Type: application/json" -d '{"username":"admin","password":"x"}' -c admin.txt
curl -s http://localhost:8080/api/orders/bob -b admin.txt
```

## Key code

**Custom session controller (simulated Redis):**

```java
@Component
public class RedisSessionController implements SessionSecurityController {
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    // isSessionExist, isSessionExpired, getUserIdBySessionId, plus createSession(userId)
}
```

**Custom user controller (simulated DB):**

```java
@Component
public class DatabaseUserController implements UserSecurityController {
    // isUserExist, getUserRoles
}
```

**Config:** BPP is wired with your session and user controllers; argument resolver builds `SecuredRequest(sessionId, pathVariableUserId, null)` so same-user is enforced for paths like `/orders/{userId}`.

**What to notice:** rest-security only enforces; you own session and user storage. Replace `RedisSessionController` with a real Redis client and `DatabaseUserController` with your DB without changing the rest-security API.
