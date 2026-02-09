package com.posadskiy.restsecurity.exception;

/** Thrown when the user has no role required by @Security. */
public final class PermissionIsAbsentException extends RuntimeException implements RestSecurityException {
}
