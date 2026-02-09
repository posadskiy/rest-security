package example.controller;

import example.service.JwtCreationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final JwtCreationService jwtCreationService;

    public AuthController(JwtCreationService jwtCreationService) {
        this.jwtCreationService = jwtCreationService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body != null ? body.get("username") : null;
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String token = jwtCreationService.createTokenForUser(username);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(Map.of("token", token));
    }
}
