package com.posadskiy.restsecurity.spring.mock;

import com.posadskiy.restsecurity.rest.SecuredRequestContext;

public interface TestClassWithClassSecurityAndPublicMethod {
    void securedMethod(SecuredRequestContext request);
    void publicMethod(SecuredRequestContext request);
}
