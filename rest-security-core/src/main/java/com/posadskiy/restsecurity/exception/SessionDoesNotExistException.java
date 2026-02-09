package com.posadskiy.restsecurity.exception;

/** Thrown when the session identifier is not known to the session store. */
public final class SessionDoesNotExistException extends RuntimeException implements RestSecurityException {
}
