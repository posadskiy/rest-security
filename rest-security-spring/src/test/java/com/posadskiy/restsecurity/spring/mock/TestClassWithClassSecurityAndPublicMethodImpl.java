package com.posadskiy.restsecurity.spring.mock;

import com.posadskiy.restsecurity.annotation.Public;
import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;

@Security(roles = {"ADMIN"})
public class TestClassWithClassSecurityAndPublicMethodImpl implements TestClassWithClassSecurityAndPublicMethod {

    @Override
    public void securedMethod(SecuredRequestContext request) {}

    @Public
    @Override
    public void publicMethod(SecuredRequestContext request) {}
}
