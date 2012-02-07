/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.user;

import java.util.List;
import java.util.Map;

import com.cloud.acl.ControlledEntity;
import com.cloud.acl.SecurityChecker.AccessType;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

/**
 * AccountManager includes logic that deals with accounts, domains, and users.
 *
 */
public interface AccountManager extends AccountService {
    /**
     * Disables an account by accountId
     * @param accountId
     * @return true if disable was successful, false otherwise
     */
    boolean disableAccount(long accountId) throws ConcurrentOperationException, ResourceUnavailableException;
    
    boolean deleteAccount(AccountVO account, long callerUserId, Account caller);

	boolean cleanupAccount(AccountVO account, long callerUserId, Account caller);

	Long checkAccessAndSpecifyAuthority(Account caller, Long zoneId);
	
	Account createAccount(String accountName, short accountType, Long domainId, String networkDomain, Map details);
	
	UserVO createUser(long accountId, String userName, String password, String firstName, String lastName, String email, String timezone);
	
    /**
     * Logs out a user
     * @param userId
     */
    void logoutUser(Long userId);

    UserAccount getUserAccount(String username, Long domainId);
    
    /**
     * Authenticates a user when s/he logs in.
     * 
     * @param username
     *            required username for authentication
     * @param password
     *            password to use for authentication, can be null for single sign-on case
     * @param domainId
     *            id of domain where user with username resides
     * @param requestParameters
     *            the request parameters of the login request, which should contain timestamp of when the request signature is
     *            made, and the signature itself in the single sign-on case
     * @return a user object, null if the user failed to authenticate
     */
    UserAccount authenticateUser(String username, String password, Long domainId, Map<String, Object[]> requestParameters);
    
    /**
     * Locate a user by their apiKey
     * 
     * @param apiKey
     *            that was created for a particular user
     * @return the user/account pair if one exact match was found, null otherwise
     */
    Pair<User, Account> findUserByApiKey(String apiKey);

    boolean lockAccount(long accountId);

	boolean enableAccount(long accountId);

	void buildACLSearchBuilder(SearchBuilder<? extends ControlledEntity> sb, Long domainId,
			boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

	void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> sc,
			Long domainId, boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

	void buildACLSearchParameters(Account caller, Long id,
			String accountName, Long projectId, List<Long> permittedAccounts, Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject, boolean listAll, boolean forProjectInvitation);
   
}
