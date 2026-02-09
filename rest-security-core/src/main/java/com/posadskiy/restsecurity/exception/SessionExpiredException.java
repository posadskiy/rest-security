package com.posadskiy.restsecurity.exception;

/** Thrown when the session has expired and must be renewed. */
public final class SessionExpiredException extends RuntimeException implements RestSecurityException {
}
