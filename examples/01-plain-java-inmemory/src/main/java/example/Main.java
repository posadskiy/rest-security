package example;

import com.posadskiy.restsecurity.context.SecurityContextHolder;
import com.posadskiy.restsecurity.enforcer.SecurityEnforcer;
import com.posadskiy.restsecurity.exception.RestSecurityException;
import com.posadskiy.restsecurity.rest.SecuredRequest;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.Map;

/**
 * Plain Java HTTP server using rest-security-core only.
 * No Spring; session and user stores are in-memory.
 */
public class Main {

    public static void main(String[] args) {
        InMemorySessionController sessionController = new InMemorySessionController();
        InMemoryUserController userController = new InMemoryUserController();
        SecurityEnforcer enforcer = new SecurityEnforcer(sessionController, userController);

        Javalin app = Javalin.create(javalinConfig -> javalinConfig.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()))).start(8080);

        // Login: returns session cookie
        app.post("/login", ctx -> {
            String body = ctx.body();
            String username = body.contains("alice") ? "alice" : body.contains("bob") ? "bob" : body.contains("admin") ? "admin" : null;
            if (username == null) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Invalid credentials"));
                return;
            }
            String sessionId = sessionController.createSession(username);
            ctx.cookie("SESSION_ID", sessionId);
            ctx.json(Map.of("sessionId", sessionId));
        });

        // Public health
        app.get("/health", ctx -> ctx.json(Map.of("status", "UP")));

        // Protected: requires USER role
        app.get("/profile", ctx -> {
            String sessionId = ctx.cookie("SESSION_ID");
            SecuredRequestContext request = new SecuredRequest(sessionId);
            try {
                String profile = enforcer.enforceAndCall(request, new String[]{"USER"}, () -> {
                    String userId = SecurityContextHolder.getContext().userId();
                    return "Profile for " + userId;
                });
                ctx.json(Map.of("profile", profile));
            } catch (RestSecurityException e) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
            }
        });

        // Protected: requires ADMIN role
        app.get("/admin", ctx -> {
            String sessionId = ctx.cookie("SESSION_ID");
            SecuredRequestContext request = new SecuredRequest(sessionId);
            try {
                enforcer.enforceAndCall(request, new String[]{"ADMIN"}, () -> {
                    return "Admin area";
                });
                ctx.json(Map.of("message", "Welcome, admin"));
            } catch (RestSecurityException e) {
                ctx.status(HttpStatus.FORBIDDEN).json(Map.of("error", e.getMessage()));
            }
        });

        System.out.println("Example 01 running at http://localhost:8080");
        System.out.println("  POST /login with body {\"username\":\"alice\",\"password\":\"pass\"}");
        System.out.println("  GET /profile with cookie SESSION_ID=<sessionId>");
        System.out.println("  GET /admin (requires ADMIN; use user admin)");
    }
}
