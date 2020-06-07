package com.posadskiy.restsecurity.controller;

import java.util.List;

public interface UserSecurityController {
	boolean isUserExist(String userId);
	List<String> getUserRoles(String userId);
}
