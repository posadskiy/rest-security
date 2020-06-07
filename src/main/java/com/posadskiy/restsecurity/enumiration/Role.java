package com.posadskiy.restsecurity.enumiration;

public enum Role {
	USER("USER"),
	ADMIN("ADMIN");

	Role(String role) {
		this.role = role;
	}

	private final String role;

	public String getRole() {
		return role;
	}
}
