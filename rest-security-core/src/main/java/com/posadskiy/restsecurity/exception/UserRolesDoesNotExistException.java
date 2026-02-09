package com.posadskiy.restsecurity.exception;

/** Thrown when the user has no roles assigned. */
public final class UserRolesDoesNotExistException extends RuntimeException implements RestSecurityException {
}
