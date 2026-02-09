package example.controller;

import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;

public interface AdminApi {

    @Security(roles = {"ADMIN"})
    String deleteUser(SecuredRequestContext request, String userId);
}
