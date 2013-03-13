// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.user;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;

import com.cloud.api.query.vo.ControlledViewEntity;
import com.cloud.exception.ConcurrentOperationException;
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
	
	Account createAccount(String accountName, short accountType, Long domainId, String networkDomain, Map details, String uuid);
	
	UserVO createUser(long accountId, String userName, String password, String firstName, String lastName, String email, String timezone, String userUUID);
	
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
    UserAccount authenticateUser(String username, String password, Long domainId, String loginIpAddress, Map<String, Object[]> requestParameters);
    
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

    void buildACLViewSearchBuilder(SearchBuilder<? extends ControlledViewEntity> sb, Long domainId,
            boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

	void buildACLSearchCriteria(SearchCriteria<? extends ControlledEntity> sc,
			Long domainId, boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

    void buildACLViewSearchCriteria(SearchCriteria<? extends ControlledViewEntity> sc,
            Long domainId, boolean isRecursive, List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria);

	void buildACLSearchParameters(Account caller, Long id,
			String accountName, Long projectId, List<Long> permittedAccounts, Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject, boolean listAll, boolean forProjectInvitation);
	
    /**
     * Deletes a user by userId
     *
     * @param accountId
     *            - id of the account do delete
     *
     * @return true if delete was successful, false otherwise
     */
    boolean deleteUserAccount(long accountId);
    
    /**
     * Updates an account
     *
     * @param cmd
     *            - the parameter containing accountId or account nameand domainId
     * @return updated account object
     */
    Account updateAccount(UpdateAccountCmd cmd);
    
    /**
     * Disables an account by accountName and domainId
     *
     * @param accountName
     * @param domainId
     * @param accountId
     * @param disabled
     *            account if success
     * @return true if disable was successful, false otherwise
     */
    Account disableAccount(String accountName, Long domainId, Long accountId) throws ConcurrentOperationException, ResourceUnavailableException;
    
    /**
     * Enables an account by accountId
     *
     * @param accountName
     *            - the enableAccount command defining the accountId to be deleted.
     * @param domainId
     *            TODO
     * @param accountId
     * @return account object
     */
    Account enableAccount(String accountName, Long domainId, Long accountId); 
    
    /**
     * Deletes user by Id
     * @param deleteUserCmd
     * @return
     */
    boolean deleteUser(DeleteUserCmd deleteUserCmd);
    
    /**
     * Update a user by userId
     *
     * @param userId
     * @return UserAccount object
     */
    UserAccount updateUser(UpdateUserCmd cmd);   
    
    /**
     * Disables a user by userId
     *
     * @param userId
     *            - the userId
     * @return UserAccount object
     */
    UserAccount disableUser(long userId);
    
    /**
     * Enables a user
     *
     * @param userId
     *            - the userId
     * @return UserAccount object
     */
    UserAccount enableUser(long userId);
    
    /**
     * Locks an account by accountId. A locked account cannot access the API, but will still have running VMs/IP
     * addresses
     * allocated/etc.
     *
     * @param accountName
     *            - the LockAccount command defining the accountId to be locked.
     * @param domainId
     *            TODO
     * @param accountId
     * @return account object
     */
    Account lockAccount(String accountName, Long domainId, Long accountId);    
}
