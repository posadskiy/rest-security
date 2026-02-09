package com.posadskiy.restsecurity.spring.mock;

import com.posadskiy.restsecurity.rest.SecuredRequestContext;

public interface TestClassWithSecurityAnnotation {
    void testMethod(SecuredRequestContext request);
}
