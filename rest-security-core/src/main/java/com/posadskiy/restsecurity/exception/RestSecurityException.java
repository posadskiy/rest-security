package com.posadskiy.restsecurity.exception;

/**
 * Sealed root for all rest-security exceptions.
 * Enables exhaustive handling in catch or switch.
 * Named to avoid clash with {@link java.lang.SecurityException}.
 */
public sealed interface RestSecurityException
        permits PermissionForGetAnotherUserIsAbsentException,
        PermissionIsAbsentException,
        SessionDoesNotExistException,
        SessionExpiredException,
        UserDoesNotExistException,
        UserRolesDoesNotExistException {
}
