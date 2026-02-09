package com.posadskiy.restsecurity.enforcer;

import com.posadskiy.restsecurity.audit.SecurityAuditListener;
import com.posadskiy.restsecurity.context.SecurityContext;
import com.posadskiy.restsecurity.context.SecurityContextHolder;
import com.posadskiy.restsecurity.controller.SessionSecurityController;
import com.posadskiy.restsecurity.controller.UserSecurityController;
import com.posadskiy.restsecurity.exception.RestSecurityException;
import com.posadskiy.restsecurity.exception.SessionDoesNotExistException;
import com.posadskiy.restsecurity.exception.SessionExpiredException;
import com.posadskiy.restsecurity.exception.UserDoesNotExistException;
import com.posadskiy.restsecurity.exception.UserRolesDoesNotExistException;
import com.posadskiy.restsecurity.exception.PermissionForGetAnotherUserIsAbsentException;
import com.posadskiy.restsecurity.exception.PermissionIsAbsentException;
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

class SecurityEnforcerTest {

    private static final String SESSION_ID = "s1";
    private static final String USER_ID = "u1";
    private static final List<String> USER_ROLES = List.of("USER");

    private SessionSecurityController sessionController;
    private UserSecurityController userController;
    private SecurityEnforcer enforcer;

    @BeforeEach
    void setUp() {
        sessionController = mock(SessionSecurityController.class);
        userController = mock(UserSecurityController.class);
        enforcer = new SecurityEnforcer(sessionController, userController);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void enforce_validSession_returnsContextAndSetsHolder() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("USER"));

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        SecurityContext ctx = enforcer.enforce(request, "USER");

        assertNotNull(ctx);
        assertEquals(USER_ID, ctx.userId());
        assertTrue(ctx.hasRole("USER"));
        assertEquals(ctx, SecurityContextHolder.getContext());

        SecurityContextHolder.clearContext();
    }

    @Test
    void enforce_invalidSession_throws() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(false);

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(SessionDoesNotExistException.class, () -> enforcer.enforce(request, "USER"));
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void enforce_nullSessionId_throwsSessionDoesNotExistException() {
        SecuredRequestContext request = new SecuredRequest(null, null, null);
        assertThrows(SessionDoesNotExistException.class, () -> enforcer.enforce(request, "USER"));
    }

    @Test
    void enforce_blankSessionId_throwsSessionDoesNotExistException() {
        SecuredRequestContext request = new SecuredRequest("  ", null, null);
        assertThrows(SessionDoesNotExistException.class, () -> enforcer.enforce(request, "USER"));
    }

    @Test
    void enforce_getUserIdBySessionIdReturnsNull_throwsUserDoesNotExistException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(null);

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        assertThrows(UserDoesNotExistException.class, () -> enforcer.enforce(request, "USER"));
    }

    @Test
    void enforce_anotherUserWithoutAdmin_throwsPermissionForGetAnotherUserIsAbsentException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("USER"));

        SecuredRequestContext request = new SecuredRequest(SESSION_ID, "other-user-id", null);
        assertThrows(PermissionForGetAnotherUserIsAbsentException.class, () -> enforcer.enforce(request, "USER"));
    }

    @Test
    void enforceAndRun_withMethodName_usesCustomNameInAudit() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("USER"));

        AtomicReference<String> methodName = new AtomicReference<>();
        enforcer.setAuditListener(new SecurityAuditListener() {
            @Override
            public void onAuthenticationSuccess(SecurityContext context, String method) {
                methodName.set(method);
            }
        });
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        enforcer.enforceAndRun(request, new String[]{"USER"}, "MyService.myMethod", () -> {});

        assertEquals("MyService.myMethod", methodName.get());
    }

    @Test
    void enforceAndCall_withMethodName_usesCustomNameInAudit() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("USER"));

        AtomicReference<String> methodName = new AtomicReference<>();
        enforcer.setAuditListener(new SecurityAuditListener() {
            @Override
            public void onAuthenticationSuccess(SecurityContext context, String method) {
                methodName.set(method);
            }
        });
        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        enforcer.enforceAndCall(request, new String[]{"USER"}, "MyApi.getData", () -> "ok");

        assertEquals("MyApi.getData", methodName.get());
    }

    @Test
    void enforceAndCall_validSession_invokesSupplierAndClearsContext() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("USER"));

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        Integer result = enforcer.enforceAndCall(request, new String[]{"USER"}, () -> {
            assertNotNull(SecurityContextHolder.getContext());
            return 42;
        });

        assertEquals(42, result);
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void enforce_expiredSession_throwsSessionExpiredException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(true);

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(SessionExpiredException.class, () -> enforcer.enforce(request, "USER"));
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void enforce_userDoesNotExist_throwsUserDoesNotExistException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(false);

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(UserDoesNotExistException.class, () -> enforcer.enforce(request, "USER"));
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void enforce_emptyRoles_throwsUserRolesDoesNotExistException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of());

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(UserRolesDoesNotExistException.class, () -> enforcer.enforce(request, "USER"));
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void enforce_wrongRole_throwsPermissionIsAbsentException() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("VIEWER"));

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(PermissionIsAbsentException.class, () -> enforcer.enforce(request, "USER"));
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void enforce_adminBypass_succeedsWithoutRequiredRole() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("ADMIN"));

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        SecurityContext ctx = enforcer.enforce(request, "SOME_OTHER_ROLE");

        assertNotNull(ctx);
        assertTrue(ctx.hasRole("ADMIN"));
        SecurityContextHolder.clearContext();
    }

    @Test
    void enforce_sameUser_succeeds() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("USER"));

        SecuredRequestContext request = new SecuredRequest(SESSION_ID, USER_ID, null);
        SecurityContext ctx = enforcer.enforce(request, "USER");

        assertNotNull(ctx);
        assertEquals(USER_ID, ctx.userId());
        SecurityContextHolder.clearContext();
    }

    @Test
    void enforceAndRun_validSession_runsActionAndClearsContext() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("USER"));

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        AtomicReference<SecurityContext> captured = new AtomicReference<>();
        enforcer.enforceAndRun(request, new String[]{"USER"}, () ->
                captured.set(SecurityContextHolder.getContext()));

        assertNotNull(captured.get());
        assertEquals(USER_ID, captured.get().userId());
        assertNull(SecurityContextHolder.getContext());
    }

    @Test
    void enforce_withAuditListener_success_callsOnAuthenticationSuccess() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(true);
        when(sessionController.isSessionExpired(SESSION_ID)).thenReturn(false);
        when(sessionController.getUserIdBySessionId(SESSION_ID)).thenReturn(USER_ID);
        when(userController.isUserExist(USER_ID)).thenReturn(true);
        when(userController.getUserRolesSet(USER_ID)).thenReturn(Set.of("USER"));

        AtomicReference<SecurityContext> successContext = new AtomicReference<>();
        AtomicReference<String> successMethod = new AtomicReference<>();
        SecurityAuditListener listener = new SecurityAuditListener() {
            @Override
            public void onAuthenticationSuccess(SecurityContext context, String method) {
                successContext.set(context);
                successMethod.set(method);
            }
        };
        enforcer.setAuditListener(listener);

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);
        enforcer.enforceWithMethodName(request, "myMethod", "USER");

        assertNotNull(successContext.get());
        assertEquals(USER_ID, successContext.get().userId());
        assertEquals("myMethod", successMethod.get());
        SecurityContextHolder.clearContext();
    }

    @Test
    void enforce_withAuditListener_failure_callsOnAuthenticationFailure() {
        when(sessionController.isSessionExist(SESSION_ID)).thenReturn(false);

        AtomicReference<String> failureSessionId = new AtomicReference<>();
        AtomicReference<String> failureMethod = new AtomicReference<>();
        AtomicReference<RestSecurityException> failureEx = new AtomicReference<>();
        SecurityAuditListener listener = new SecurityAuditListener() {
            @Override
            public void onAuthenticationFailure(String sessionId, String method, RestSecurityException exception) {
                failureSessionId.set(sessionId);
                failureMethod.set(method);
                failureEx.set(exception);
            }
        };
        enforcer.setAuditListener(listener);

        SecuredRequestContext request = new SecuredRequest(SESSION_ID);

        assertThrows(SessionDoesNotExistException.class, () -> enforcer.enforceWithMethodName(request, "myMethod", "USER"));
        assertEquals(SESSION_ID, failureSessionId.get());
        assertEquals("myMethod", failureMethod.get());
        assertTrue(failureEx.get() instanceof SessionDoesNotExistException);
    }
}
