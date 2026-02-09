package com.posadskiy.restsecurity.spring.mock;

import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;

public class TestClassWithSecurityAnnotationImpl implements TestClassWithSecurityAnnotation {

    @Security
    @Override
    public void testMethod(SecuredRequestContext request) {}
}
