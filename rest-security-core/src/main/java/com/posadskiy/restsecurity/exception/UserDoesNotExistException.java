package com.posadskiy.restsecurity.exception;

/** Thrown when the user resolved from the session does not exist. */
public final class UserDoesNotExistException extends RuntimeException implements RestSecurityException {
}
