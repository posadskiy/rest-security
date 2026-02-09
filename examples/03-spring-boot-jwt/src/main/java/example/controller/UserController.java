package example.controller;

import com.posadskiy.restsecurity.context.SecurityContextHolder;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import example.dto.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController implements UserApi {

    @Override
    @GetMapping("/me")
    public UserProfile getProfile(SecuredRequestContext request) {
        var ctx = SecurityContextHolder.getContext();
        return new UserProfile(ctx.userId(), ctx.roles());
    }

    @Override
    @PutMapping("/{userId}")
    public void updateProfile(SecuredRequestContext request, @PathVariable String userId) {
        var ctx = SecurityContextHolder.getContext();
        if (!ctx.userId().equals(userId) && !ctx.roles().contains("ADMIN")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Same-user or ADMIN required");
        }
        // In a real app: update user in DB
    }
}
