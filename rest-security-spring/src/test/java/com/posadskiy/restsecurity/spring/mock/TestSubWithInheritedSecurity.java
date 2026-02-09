package com.posadskiy.restsecurity.spring.mock;

/**
 * Subclass that inherits the @Security method from TestBaseWithSecurityMethodImpl.
 * Implements the interface so the bean gets proxied; method is inherited from parent.
 */
public class TestSubWithInheritedSecurity extends TestBaseWithSecurityMethodImpl implements TestBaseWithSecurityMethod {
}
