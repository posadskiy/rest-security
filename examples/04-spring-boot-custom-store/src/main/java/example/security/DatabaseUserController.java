package example.security;

import com.posadskiy.restsecurity.controller.UserSecurityController;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User and role store implementing UserSecurityController.
 * Simulates a database with an in-memory map; not for production.
 */
@Component
public class DatabaseUserController implements UserSecurityController {

    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();

    public DatabaseUserController() {
        users.put("alice", new UserRecord("alice", List.of("USER")));
        users.put("bob", new UserRecord("bob", List.of("USER", "EDITOR")));
        users.put("admin", new UserRecord("admin", List.of("ADMIN")));
    }

    @Override
    public boolean isUserExist(String userId) {
        return userId != null && !userId.isBlank() && users.containsKey(userId);
    }

    @Override
    public List<String> getUserRoles(String userId) {
        UserRecord u = users.get(userId);
        return u != null ? u.roles() : List.of();
    }

    private record UserRecord(String id, List<String> roles) {}
}
