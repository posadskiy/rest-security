# Example 02: JWT with lightweight HTTP server

## What this demonstrates

- **rest-security-jwt**: `JwtSecurityController` with HMAC-signed tokens.
- No session store or user DB — user and roles come from JWT claims.
- Login endpoint creates a JWT; protected endpoints validate it via `SecurityEnforcer`.
- `trustGateway=false` (default): signature is verified.

## How to run

From the project root, install the library, then:

```bash
cd examples/02-jwt-lightweight-http
mvn exec:java
```

Server starts at http://localhost:8080.

## Try it

```bash
# Get a token (e.g. for bob)
TOKEN=$(curl -s -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"username":"bob","password":"pass"}' | jq -r .token)

# Call protected endpoint
curl -s http://localhost:8080/users -H "Authorization: Bearer $TOKEN"

# Admin (login as admin first)
TOKEN=$(curl -s -X POST http://localhost:8080/login -H "Content-Type: application/json" -d '{"username":"admin","password":"pass"}' | jq -r .token)
curl -s http://localhost:8080/admin -H "Authorization: Bearer $TOKEN"
```

## Key code

**Config and enforcer (JWT is both session and user source):**

```java
JwtConfig config = JwtConfig.withSecret("my-hmac-secret...");
JwtSecurityController jwt = new JwtSecurityController(config);
SecurityEnforcer enforcer = new SecurityEnforcer(jwt, jwt);
```

**Protected route — pass token as request identity:**

```java
String token = ctx.header("Authorization").replace("Bearer ", "");
enforcer.enforceAndCall(new SecuredRequest(token), new String[]{"USER"}, () -> listUsers());
```

**What to notice:** One token carries session + user + roles; no database lookup.
