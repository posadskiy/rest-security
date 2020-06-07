package com.posadskiy.restsecurity.annotation.bpp.mock;

import com.posadskiy.restsecurity.annotation.Security;
import com.posadskiy.restsecurity.rest.RequestWrapper;

public class TestClassWithSecurityAnnotationImpl implements TestClassWithSecurityAnnotation {

	@Security
	public void testMethod(RequestWrapper requestWrapper) {}
}
