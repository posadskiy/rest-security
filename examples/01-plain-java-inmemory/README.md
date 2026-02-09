# Example 01: Plain Java with in-memory session store

## What this demonstrates

- Using **rest-security-core** only (no Spring, no JWT).
- Implementing `SessionSecurityController` and `UserSecurityController` with in-memory stores (`ConcurrentHashMap`).
- Using `SecurityEnforcer.enforceAndCall()` in a plain Java HTTP server (Javalin).
- Role-based access: `/profile` requires USER, `/admin` requires ADMIN.

## How to run

From the **rest-security project root**, install the library first:

```bash
mvn clean install -DskipTests
```

Then from this directory:

```bash
cd examples/01-plain-java-inmemory
mvn exec:java
```

Server starts at http://localhost:8080.

## Try it

```bash
# Login as alice (USER)
curl -s -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"username":"alice","password":"pass"}' -c cookies.txt

# Get profile (use session from cookie)
curl -s http://localhost:8080/profile -b cookies.txt

# Login as admin and hit admin endpoint
curl -s -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"username":"admin","password":"pass"}' -c admin.txt
curl -s http://localhost:8080/admin -b admin.txt
```

## Key code

**Enforcing security in a route:**

```java
SecuredRequestContext request = new SecuredRequest(sessionId);
String result = enforcer.enforceAndCall(request, new String[]{"USER"}, () -> {
    String userId = SecurityContextHolder.getContext().userId();
    return getProfile(userId);
});
```

**What to notice:** `enforceAndCall` clears `SecurityContextHolder` in a `finally` block, so you don't need to call `clearContext()` yourself.

## Sample users

| Username | Password | Roles   |
|----------|----------|---------|
| alice    | (any)    | USER    |
| bob      | (any)    | USER, EDITOR |
| admin    | (any)    | ADMIN   |
