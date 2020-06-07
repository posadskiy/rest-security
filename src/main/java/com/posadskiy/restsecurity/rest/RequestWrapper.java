package com.posadskiy.restsecurity.rest;

public class RequestWrapper {
	private Object data;
	private String userId;
	private String sessionId;

	public RequestWrapper data(Object data) {
		this.data = data;
		return this;
	}

	public RequestWrapper userId(String userId) {
		this.userId = userId;
		return this;
	}

	public RequestWrapper sessionId(String sessionId) {
		this.sessionId = sessionId;
		return this;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getUserId() {
		return userId;
	}

	public Object getData() {
		return data;
	}
}
