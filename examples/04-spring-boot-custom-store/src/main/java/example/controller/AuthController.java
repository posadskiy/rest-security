package example.controller;

import example.security.DatabaseUserController;
import example.security.RedisSessionController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final RedisSessionController sessionController;
    private final DatabaseUserController userController;

    public AuthController(RedisSessionController sessionController, DatabaseUserController userController) {
        this.sessionController = sessionController;
        this.userController = userController;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body,
                                                      HttpServletResponse response) {
        String username = body != null ? body.get("username") : null;
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (!userController.isUserExist(username)) {
            return ResponseEntity.status(401).build();
        }
        String sessionId = sessionController.createSession(username);
        response.addHeader("Set-Cookie", "SESSION_ID=" + sessionId + "; Path=/; HttpOnly");
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }
}
