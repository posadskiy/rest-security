package com.posadskiy.restsecurity.controller;

public interface SessionSecurityController {
	boolean isSessionExist(String sessionId);
	boolean isSessionExpired(String sessionId);
	String getUserIdBySessionId(String sessionId);
}
