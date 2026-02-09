package example.controller;

import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController implements AdminApi {

    @Override
    @DeleteMapping("/users/{userId}")
    public String deleteUser(SecuredRequestContext request, @PathVariable String userId) {
        return "Deleted user " + userId;
    }
}
