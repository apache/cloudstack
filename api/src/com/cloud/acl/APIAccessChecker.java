package com.cloud.acl;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.Adapter;

/**
 * APIAccessChecker checks the ownership and access control to API requests 
 */
public interface APIAccessChecker extends Adapter {
	
	boolean canAccessAPI(User user, String apiCommandName) throws PermissionDeniedException;
}
