package com.posadskiy.restsecurity.exception;

/** Thrown when a non-admin user attempts to access another user's resource. */
public final class PermissionForGetAnotherUserIsAbsentException extends RuntimeException implements RestSecurityException {
}
