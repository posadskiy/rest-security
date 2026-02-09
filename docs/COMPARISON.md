# Spring Security vs rest-security

A fair comparison to help you choose the right tool. Both are valid; they target different needs.

---

## When to use Spring Security

Use **Spring Security** when you need:

- **Full authentication flows**: login forms, OAuth2/OIDC, SAML, LDAP, Kerberos
- **Built-in session management**: session fixation protection, concurrent session control, remember-me
- **HTTP-specific features**: CSRF tokens, CORS, security headers, channel security
- **Reactive stack**: Spring WebFlux with reactive security context
- **Method security with expressions**: `@PreAuthorize("hasRole('ADMIN') and #id == principal.id")`
- **Enterprise integration**: ACLs, LDAP, Active Directory, JAAS

Spring Security is the right choice when authentication and session lifecycle are your responsibility and you need the full toolkit.

---

## When to use rest-security

Use **rest-security** when:

- **You already know who the user is**: a gateway or auth service did the login; you only need to enforce roles and same-user access.
- **You want minimal dependencies**: core has zero dependencies; no Spring required for the security logic.
- **You're not on HTTP**: batch jobs, message consumers, CLI tools, gRPC, GraphQL, or desktop apps with Spring DI.
- **You're building an MVP**: you need "protect this method by role" without configuring filter chains and auth providers.
- **You're teaching or learning**: the entire core is a few hundred lines you can read in 15 minutes.
- **You use Micronaut, Quarkus, or plain Java**: use the core module and call `SecurityEnforcer` directly; no Spring.

**Tagline**: *Authorization without the authentication overhead.*

---

## Audience guide

| Audience | Use rest-security when… |
|---------|---------------------------|
| **Microservices behind a gateway** | The gateway validates JWT/session; your service only needs "does this user have the role?" and "can they access this user's data?" |
| **Non-HTTP Spring apps** | Spring Batch, Kafka/Rabbit consumers, scheduled jobs, CLI tools. Spring Security is built around HTTP filters; rest-security is method-level. |
| **gRPC / GraphQL** | You need interceptors or directives that enforce roles; no servlet filter model. |
| **Desktop / Swing / JavaFX** | You use Spring for DI; you need "this action requires ADMIN" without any web stack. |
| **MVPs and startups** | Simple CRUD API with login elsewhere; you just need role checks and maybe JWT parsing. |
| **Non-Spring Java** | Plain Java, Micronaut, Quarkus. Use `rest-security-core` and `SecurityEnforcer`; no Spring. |
| **Educational** | You want to understand what "secure by role" means before adopting a large framework. |

---

## Feature comparison

- **Setup complexity**: rest-security — implement 2 interfaces (session + user), register 1 bean (Spring) or use `SecurityEnforcer` directly. Spring Security — filter chain, security context, one or more auth providers, often extensive configuration.
- **Dependencies**: rest-security-core has zero; rest-security-spring adds Spring Context only. Spring Security pulls in many transitive dependencies.
- **Learning curve**: rest-security — one main class to read (`SecurityEnforcer`). Spring Security — large documentation surface and many concepts.
- **JWT**: rest-security-jwt — validate HMAC-signed JWT and read user/roles from claims; optional trust-gateway mode. Spring Security — full OAuth2 resource server with JWT, issuer validation, key resolution.
- **Session management**: rest-security — bring your own (you implement `SessionSecurityController`). Spring Security — built-in session handling and options.
- **Framework coupling**: rest-security-core — none. rest-security-spring — Spring Context. Spring Security — Spring MVC/WebFlux and servlet/reactive APIs.

---

## Code comparison: protect endpoint by role

**rest-security (Spring):**

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityAnnotationBeanPostProcessor securityProcessor(
            SessionSecurityController session,
            UserSecurityController user) {
        return new SecurityAnnotationBeanPostProcessor(session, user);
    }
}

public interface UserApi {
    @Security(roles = {"USER"})
    UserProfile getProfile(SecuredRequestContext request);
}

// In handler: request is e.g. new SecuredRequest(sessionId) or new SecuredRequest(jwtToken)
```

**rest-security (plain Java, no Spring):**

```java
var enforcer = new SecurityEnforcer(sessionController, userController);
SecuredRequestContext request = new SecuredRequest(sessionId);
SecurityContext ctx = enforcer.enforce(request, "USER");
// use ctx.userId(), ctx.roles()
```

**Spring Security (typical):**

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/profile").hasRole("USER")
                .anyRequest().authenticated())
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            // or .formLogin(), .httpBasic(), etc.
            .build();
    }
}

// Plus JWT config, user details, etc., depending on your auth method.
```

Use the comparison that matches your setup (REST with session, REST with JWT, or non-HTTP).

---

## Migration path

- **Start with rest-security** when you need simple role-based protection and optional JWT parsing. Get to production quickly.
- **Graduate to Spring Security** when you need OAuth2/OIDC, SAML, built-in session management, or reactive security. Your existing `SessionSecurityController` / `UserSecurityController` concepts map to custom Spring Security components if you migrate later.

You can also use both in the same organization: rest-security in small or non-HTTP services, Spring Security in the main web app or identity provider.
