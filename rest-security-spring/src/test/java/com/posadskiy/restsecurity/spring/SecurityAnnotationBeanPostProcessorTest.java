package com.posadskiy.restsecurity.spring;

import com.posadskiy.restsecurity.spring.mock.TestBaseWithSecurityMethod;
import com.posadskiy.restsecurity.spring.mock.TestClassWithClassSecurityAndPublicMethod;
import com.posadskiy.restsecurity.spring.mock.TestClassWithClassSecurityAndPublicMethodImpl;
import com.posadskiy.restsecurity.spring.mock.TestClassWithSecurityAnnotation;
import com.posadskiy.restsecurity.spring.mock.TestClassWithSecurityAnnotationImpl;
import com.posadskiy.restsecurity.spring.mock.TestClassWithoutSecurityAnnotation;
import com.posadskiy.restsecurity.spring.mock.TestSubWithInheritedSecurity;
import com.posadskiy.restsecurity.audit.SecurityAuditListener;
import com.posadskiy.restsecurity.context.SecurityContext;
import com.posadskiy.restsecurity.context.SecurityContextHolder;
import com.posadskiy.restsecurity.controller.SessionSecurityController;
import com.posadskiy.restsecurity.controller.UserSecurityController;
import com.posadskiy.restsecurity.exception.RestSecurityException;
import com.posadskiy.restsecurity.exception.*;
import com.posadskiy.restsecurity.rest.SecuredRequest;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityAnnotationBeanPostProcessorTest {

    private static final String SESSION_ID = "SESSION_ID";
    private static final String USER_ID = "USER_ID";
    private static final String ANOTHER_USER_ID = "ANOTHER_USER_ID";
    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String BAD_ROLE = "BAD_ROLE";
    private static final List<String> EMPTY_ROLES = List.of();
    private static final List<String> BAD_ROLES = List.of(BAD_ROLE);
    private static final List<String> USER_ROLES = List.of(USER_ROLE);
    private static final List<String> ADMIN_ROLES = List.of(ADMIN_ROLE);

    private SecurityAnnotationBeanPostProcessor postProcessor;
    private SessionSecurityController sessionController;
    private UserSecurityController userController;

    @BeforeEach
    void setUp() {
        sessionController = mock(SessionSecurityController.class);
        userController = mock(UserSecurityController.class);
        postProcessor = new SecurityAnnotationBeanPostProcessor(sessionController, userController);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void postProcess_beanWithAnnotation_proxyReturned() {
        var bean = new TestClassWithSecurityAnnotationImpl();
        String beanName = "testBean";

        Object before = postProcessor.postProcessBeforeInitialization(bean, beanName);
        Object after = postProcessor.postProcessAfterInitialization(before, beanName);

        assertNotEquals(bean.getClass().getSimpleName(), after.getClass().getSimpleName());
    }

    @Test
    void postProcess_beanWithoutAnnotation_originalBeanReturned() {
        var bean = new TestClassWithoutSecurityAnnotation();
        String beanName = "testBean";

        Object before = postProcessor.postProcessBeforeInitialization(bean, beanName);
        Object after = postProcessor.postProcessAfterInitialization(before, beanName);

        assertEquals(bean.getClass().getSimpleName(), after.getClass().getSimpleName());
    }

    @Test
    void postProcess_sessionDoesNotExist_throwsSessionDoesNotExistException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(false);

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(SessionDoesNotExistException.class, () -> bean.testMethod(request));
    }

    @Test
    void postProcess_sessionExpired_throwsSessionExpiredException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(true);

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(SessionExpiredException.class, () -> bean.testMethod(request));
    }

    @Test
    void postProcess_userDoesNotExist_throwsUserDoesNotExistException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(false);

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(UserDoesNotExistException.class, () -> bean.testMethod(request));
    }

    @Test
    void postProcess_userRolesEmpty_throwsUserRolesDoesNotExistException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRoles(USER_ID)).thenReturn(EMPTY_ROLES);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of());

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(UserRolesDoesNotExistException.class, () -> bean.testMethod(request));
    }

    @Test
    void postProcess_userWithoutRequiredRole_throwsPermissionIsAbsentException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRoles(USER_ID)).thenReturn(BAD_ROLES);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(BAD_ROLE));

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(PermissionIsAbsentException.class, () -> bean.testMethod(request));
    }

    @Test
    void postProcess_requestAnotherUserWithoutAdmin_throwsPermissionForGetAnotherUserIsAbsentException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRoles(USER_ID)).thenReturn(USER_ROLES);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(USER_ROLE));

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID, ANOTHER_USER_ID, null);

        assertThrows(PermissionForGetAnotherUserIsAbsentException.class, () -> bean.testMethod(request));
    }

    @Test
    void postProcess_adminCanAccessAnotherUser_succeeds() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRoles(USER_ID)).thenReturn(ADMIN_ROLES);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(ADMIN_ROLE));

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID, ANOTHER_USER_ID, null);

        assertDoesNotThrow(() -> bean.testMethod(request));
    }

    @Test
    void postProcess_securityContextClearedAfterInvocation() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRoles(USER_ID)).thenReturn(USER_ROLES);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(USER_ROLE));

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        bean.testMethod(request);

        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void postProcess_publicMethod_skipsSecurityEvenWhenClassHasSecurity() {
        var impl = new TestClassWithClassSecurityAndPublicMethodImpl();
        Object before = postProcessor.postProcessBeforeInitialization(impl, "adminBean");
        TestClassWithClassSecurityAndPublicMethod bean = (TestClassWithClassSecurityAndPublicMethod)
                postProcessor.postProcessAfterInitialization(before, "adminBean");

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertDoesNotThrow(() -> bean.publicMethod(request));
    }

    @Test
    void postProcess_classLevelSecurity_securedMethodRequiresAuth_throwsWhenWrongRole() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(USER_ROLE));

        var impl = new TestClassWithClassSecurityAndPublicMethodImpl();
        Object before = postProcessor.postProcessBeforeInitialization(impl, "adminBean");
        TestClassWithClassSecurityAndPublicMethod bean = (TestClassWithClassSecurityAndPublicMethod)
                postProcessor.postProcessAfterInitialization(before, "adminBean");

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(PermissionIsAbsentException.class, () -> bean.securedMethod(request));
    }

    @Test
    void postProcess_classLevelSecurity_securedMethodRequiresAuth_succeedsWithAdmin() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(ADMIN_ROLE));

        var impl = new TestClassWithClassSecurityAndPublicMethodImpl();
        Object before = postProcessor.postProcessBeforeInitialization(impl, "adminBean");
        TestClassWithClassSecurityAndPublicMethod bean = (TestClassWithClassSecurityAndPublicMethod)
                postProcessor.postProcessAfterInitialization(before, "adminBean");

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertDoesNotThrow(() -> bean.securedMethod(request));
    }

    @Test
    void postProcess_inheritedMethod_resolvesAndEnforcesSecurity() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(USER_ROLE));

        var impl = new TestSubWithInheritedSecurity();
        Object before = postProcessor.postProcessBeforeInitialization(impl, "inheritedBean");
        TestBaseWithSecurityMethod bean = (TestBaseWithSecurityMethod)
                postProcessor.postProcessAfterInitialization(before, "inheritedBean");

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertDoesNotThrow(() -> bean.securedInheritedMethod(request));
    }

    @Test
    void postProcess_securedMethodWithNullArgs_throwsIllegalArgumentException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(USER_ROLE));

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        assertThrows(IllegalArgumentException.class, () -> bean.testMethod(null));
    }

    @Test
    void postProcess_withAuditListener_success_callsOnAuthenticationSuccess() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of(USER_ROLE));

        AtomicReference<SecurityContext> successContext = new AtomicReference<>();
        AtomicReference<String> successMethod = new AtomicReference<>();
        SecurityAuditListener listener = new SecurityAuditListener() {
            @Override
            public void onAuthenticationSuccess(SecurityContext context, String method) {
                successContext.set(context);
                successMethod.set(method);
            }
        };
        postProcessor.setAuditListener(listener);

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        bean.testMethod(request);

        assertNotNull(successContext.get());
        assertEquals(USER_ID, successContext.get().userId());
        assertNotNull(successMethod.get());
        assertTrue(successMethod.get().contains("testMethod"));
    }

    @Test
    void postProcess_withAuditListener_failure_callsOnAuthenticationFailure() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(false);

        AtomicReference<String> failSessionId = new AtomicReference<>();
        AtomicReference<RestSecurityException> failEx = new AtomicReference<>();
        SecurityAuditListener listener = new SecurityAuditListener() {
            @Override
            public void onAuthenticationFailure(String sessionId, String method, RestSecurityException exception) {
                failSessionId.set(sessionId);
                failEx.set(exception);
            }
        };
        postProcessor.setAuditListener(listener);

        var bean = postProcessBean(new TestClassWithSecurityAnnotationImpl(), "b");
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(SessionDoesNotExistException.class, () -> bean.testMethod(request));
        assertEquals(SESSION_ID, failSessionId.get());
        assertNotNull(failEx.get());
    }

    private TestClassWithSecurityAnnotation postProcessBean(TestClassWithSecurityAnnotationImpl impl, String name) {
        Object before = postProcessor.postProcessBeforeInitialization(impl, name);
        Object after = postProcessor.postProcessAfterInitialization(before, name);
        return (TestClassWithSecurityAnnotation) after;
    }
}
