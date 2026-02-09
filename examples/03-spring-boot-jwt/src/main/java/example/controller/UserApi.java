package example.controller;

import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import example.dto.UserProfile;

public interface UserApi {

    @Security(roles = {"USER"})
    UserProfile getProfile(SecuredRequestContext request);

    @Security(roles = {"USER"})
    void updateProfile(SecuredRequestContext request, String userId);
}
