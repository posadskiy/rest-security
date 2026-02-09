package example;

import com.posadskiy.restsecurity.controller.UserSecurityController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory user and role store implementing UserSecurityController.
 * Simulates a user store (e.g. database); not for production.
 */
public class InMemoryUserController implements UserSecurityController {

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public InMemoryUserController() {
        users.put("alice", new User("alice", List.of("USER")));
        users.put("bob", new User("bob", List.of("USER", "EDITOR")));
        users.put("admin", new User("admin", List.of("ADMIN")));
    }

    @Override
    public boolean isUserExist(String userId) {
        return userId != null && !userId.isBlank() && users.containsKey(userId);
    }

    @Override
    public List<String> getUserRoles(String userId) {
        User user = users.get(userId);
        return user != null ? user.roles() : List.of();
    }

    @Override
    public Set<String> getUserRolesSet(String userId) {
        return Set.copyOf(getUserRoles(userId));
    }

    private record User(String id, List<String> roles) {}
}
