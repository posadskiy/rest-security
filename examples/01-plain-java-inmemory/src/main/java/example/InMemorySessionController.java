package example;

import com.posadskiy.restsecurity.controller.SessionSecurityController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store implementing SessionSecurityController.
 * Simulates a session store (e.g. Redis); not for production.
 */
public class InMemorySessionController implements SessionSecurityController {

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    @Override
    public boolean isSessionExist(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        SessionData data = sessions.get(sessionId);
        return data != null && !data.expiresAt().isBefore(Instant.now());
    }

    @Override
    public boolean isSessionExpired(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return true;
        SessionData data = sessions.get(sessionId);
        return data == null || data.expiresAt().isBefore(Instant.now());
    }

    @Override
    public String getUserIdBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        SessionData data = sessions.get(sessionId);
        if (data == null || data.expiresAt().isBefore(Instant.now())) return null;
        return data.userId();
    }

    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, new SessionData(userId, Instant.now().plusSeconds(3600)));
        return sessionId;
    }

    private record SessionData(String userId, Instant expiresAt) {}
}
