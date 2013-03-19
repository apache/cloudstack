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
package org.apache.cloudstack.region;

import java.util.List;

import org.apache.cloudstack.api.command.admin.account.DeleteAccountCmd;
import org.apache.cloudstack.api.command.admin.account.DisableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.EnableAccountCmd;
import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.domain.DeleteDomainCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.DisableUserCmd;
import org.apache.cloudstack.api.command.admin.user.EnableUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;
import org.apache.cloudstack.api.command.user.region.ListRegionsCmd;

import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;


public interface RegionService {
	/**
	 * Adds a Region to the local Region
	 * @param id
	 * @param name
	 * @param endPoint
	 * @return Return added Region object
	 */
	public Region addRegion(int id, String name, String endPoint);
	
	/**
	 * Update details of the Region with specified Id
	 * @param id
	 * @param name
	 * @param endPoint
	 * @return Return updated Region object
	 */
	public Region updateRegion(int id, String name, String endPoint);
	
	/**
	 * @param id
	 * @return True if region is successfully removed
	 */
	public boolean removeRegion(int id);
	
	/** List all Regions or by Id/Name
	 * @param id
	 * @param name
	 * @return List of Regions
	 */
	public List<? extends Region> listRegions(ListRegionsCmd cmd);
	
    /**
     * Deletes a user by userId
     * isPopagate flag is set to true if sent from peer Region
     * @param cmd
     *
     * @return true if delete was successful, false otherwise
     */
	boolean deleteUserAccount(DeleteAccountCmd cmd);
	
    /**
     * Updates an account
     * isPopagate falg is set to true if sent from peer Region 
     *
     * @param cmd
     *            - the parameter containing accountId or account nameand domainId
     * @return updated account object
     */
    Account updateAccount(UpdateAccountCmd cmd);
	
	/**
	 * Disables an account by accountName and domainId or accountId
	 * @param cmd
	 * @return
	 * @throws ResourceUnavailableException 
	 * @throws ConcurrentOperationException 
	 */
	Account disableAccount(DisableAccountCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException;
	
	/**
	 * Enables an account by accountId
	 * @param cmd
	 * @return
	 */
	Account enableAccount(EnableAccountCmd cmd);

    /**
     * Deletes user by Id
     * @param deleteUserCmd
     * @return true if delete was successful, false otherwise
     */
    boolean deleteUser(DeleteUserCmd deleteUserCmd); 	
    
    /**
     * update an existing domain
     * 
     * @param cmd
     *            - the command containing domainId and new domainName
     * @return Domain object if the command succeeded
     */
	public Domain updateDomain(UpdateDomainCmd updateDomainCmd);    
    
	/**
	 * Deletes domain
	 * @param cmd
	 * @return true if delete was successful, false otherwise
	 */
	public boolean deleteDomain(DeleteDomainCmd cmd);
	
    /**
     * Update a user by userId
     *
     * @param userId
     * @return UserAccount object
     */
	public UserAccount updateUser(UpdateUserCmd updateUserCmd);
	
    /**
     * Disables a user by userId
     *
     * @param cmd
     * @return UserAccount object
     */
	public UserAccount disableUser(DisableUserCmd cmd);
	
    /**
     * Enables a user
     *
     * @param cmd
     * @return UserAccount object
     */
	public UserAccount enableUser(EnableUserCmd cmd);
}
