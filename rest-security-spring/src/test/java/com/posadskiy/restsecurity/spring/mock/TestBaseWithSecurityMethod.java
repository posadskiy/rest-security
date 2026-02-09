package com.posadskiy.restsecurity.spring.mock;

import com.posadskiy.restsecurity.rest.SecuredRequestContext;

public interface TestBaseWithSecurityMethod {
    void securedInheritedMethod(SecuredRequestContext request);
}
