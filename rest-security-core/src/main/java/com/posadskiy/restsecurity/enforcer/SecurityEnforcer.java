package com.posadskiy.restsecurity.enforcer;

import com.posadskiy.restsecurity.audit.SecurityAuditListener;
import com.posadskiy.restsecurity.context.SecurityContext;
import com.posadskiy.restsecurity.context.SecurityContextHolder;
import com.posadskiy.restsecurity.controller.SessionSecurityController;
import com.posadskiy.restsecurity.controller.UserSecurityController;
import com.posadskiy.restsecurity.enumeration.Role;
import com.posadskiy.restsecurity.exception.*;
import com.posadskiy.restsecurity.rest.SecuredRequestContext;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Framework-agnostic security enforcer.
 * Works with any Java application — no Spring, no servlets required.
 * Validates session, user, and roles; populates {@link SecurityContextHolder} on success.
 */
public final class SecurityEnforcer {

    private final SessionSecurityController sessionController;
    private final UserSecurityController userController;
    private SecurityAuditListener auditListener;

    public SecurityEnforcer(SessionSecurityController sessionController,
                            UserSecurityController userController) {
        this.sessionController = sessionController;
        this.userController = userController;
    }

    /**
     * Optional audit listener (e.g. for logging). Not used by default.
     */
    public void setAuditListener(SecurityAuditListener auditListener) {
        this.auditListener = auditListener;
    }

    /**
     * Validate session and roles. On success, populates {@link SecurityContextHolder} and returns the context.
     * Caller should call {@link SecurityContextHolder#clearContext()} when done, or use {@link #enforceAndRun}
     * / {@link #enforceAndCall} which clear automatically.
     * Audit listener is notified on success and on failure when set.
     *
     * @param ctx            secured request context (session id, optional target user)
     * @param requiredRoles  roles required (user must have at least one, unless ADMIN)
     * @return validated security context
     * @throws RuntimeException a subtype of RestSecurityException on validation failure
     */
    public SecurityContext enforce(SecuredRequestContext ctx, String... requiredRoles) {
        return enforceWithMethodName(ctx, "enforce", requiredRoles);
    }

    /**
     * Like {@link #enforce(SecuredRequestContext, String...)} with a method name for audit logging.
     */
    public SecurityContext enforceWithMethodName(SecuredRequestContext ctx, String methodName, String... requiredRoles) {
        String sessionId = ctx.getSessionId();
        try {
            SecurityContext securityContext = validateAndBuildContext(ctx, requiredRoles);
            SecurityContextHolder.setContext(securityContext);
            if (auditListener != null) {
                auditListener.onAuthenticationSuccess(securityContext, methodName);
            }
            return securityContext;
        } catch (RuntimeException e) {
            if (e instanceof RestSecurityException secEx && auditListener != null) {
                auditListener.onAuthenticationFailure(sessionId, methodName, secEx);
            }
            throw e;
        }
    }

    /**
     * Validate, run the action with context set, then clear context. Clears context in finally.
     *
     * @param ctx           secured request context
     * @param requiredRoles roles required
     * @param action        runnable to execute after validation
     */
    public void enforceAndRun(SecuredRequestContext ctx, String[] requiredRoles, Runnable action) {
        enforceAndRun(ctx, requiredRoles, "enforceAndRun", action);
    }

    /**
     * Like {@link #enforceAndRun(SecuredRequestContext, String[], Runnable)} with a method name for audit logging.
     */
    public void enforceAndRun(SecuredRequestContext ctx, String[] requiredRoles, String methodName, Runnable action) {
        String sessionId = ctx.getSessionId();
        try {
            SecurityContext securityContext = validateAndBuildContext(ctx, requiredRoles);
            SecurityContextHolder.setContext(securityContext);
            if (auditListener != null) {
                auditListener.onAuthenticationSuccess(securityContext, methodName);
            }
            action.run();
        } catch (RuntimeException e) {
            if (e instanceof RestSecurityException secEx && auditListener != null) {
                auditListener.onAuthenticationFailure(sessionId, methodName, secEx);
            }
            throw e;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Validate, call the action with context set, return result, then clear context. Clears context in finally.
     *
     * @param ctx           secured request context
     * @param requiredRoles roles required
     * @param action        supplier to call after validation
     * @return result of action
     */
    public <T> T enforceAndCall(SecuredRequestContext ctx, String[] requiredRoles, Supplier<T> action) {
        return enforceAndCall(ctx, requiredRoles, "enforceAndCall", action);
    }

    /**
     * Like {@link #enforceAndCall(SecuredRequestContext, String[], Supplier)} with a method name for audit logging.
     */
    public <T> T enforceAndCall(SecuredRequestContext ctx, String[] requiredRoles, String methodName, Supplier<T> action) {
        String sessionId = ctx.getSessionId();
        try {
            SecurityContext securityContext = validateAndBuildContext(ctx, requiredRoles);
            SecurityContextHolder.setContext(securityContext);
            if (auditListener != null) {
                auditListener.onAuthenticationSuccess(securityContext, methodName);
            }
            return action.get();
        } catch (RuntimeException e) {
            if (e instanceof RestSecurityException secEx && auditListener != null) {
                auditListener.onAuthenticationFailure(sessionId, methodName, secEx);
            }
            throw e;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Validate only. Does not set SecurityContextHolder. Returns context on success.
     * Useful when you need to check without populating the holder.
     */
    public SecurityContext validateAndBuildContext(SecuredRequestContext ctx, String... requiredRoles) {
        String sessionId = ctx.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new SessionDoesNotExistException();
        }
        if (!sessionController.isSessionExist(sessionId)) {
            throw new SessionDoesNotExistException();
        }
        if (sessionController.isSessionExpired(sessionId)) {
            throw new SessionExpiredException();
        }

        String userId = sessionController.getUserIdBySessionId(sessionId);
        if (userId == null || userId.isBlank()) {
            throw new UserDoesNotExistException();
        }
        if (!userController.isUserExist(userId)) {
            throw new UserDoesNotExistException();
        }

        Set<String> userRoles = userController.getUserRolesSet(userId);
        if (userRoles.isEmpty()) {
            throw new UserRolesDoesNotExistException();
        }

        Set<String> required = Set.of(requiredRoles);
        boolean isAdmin = userRoles.stream().anyMatch(Role::isAdmin);

        if (!isAdmin) {
            boolean hasRole = userRoles.stream().anyMatch(required::contains);
            if (!hasRole) {
                throw new PermissionIsAbsentException();
            }
            String targetUserId = ctx.getUserId();
            if (targetUserId != null && !targetUserId.equals(userId)) {
                throw new PermissionForGetAnotherUserIsAbsentException();
            }
        }

        return new SecurityContext(sessionId, userId, userRoles);
    }
}
