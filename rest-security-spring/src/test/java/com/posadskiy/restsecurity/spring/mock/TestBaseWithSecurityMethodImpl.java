package com.posadskiy.restsecurity.spring.mock;

import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;

public class TestBaseWithSecurityMethodImpl implements TestBaseWithSecurityMethod {

    @Security(roles = {"USER"})
    @Override
    public void securedInheritedMethod(SecuredRequestContext request) {}
}
