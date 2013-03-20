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
import java.util.Map;

import org.apache.cloudstack.api.command.admin.account.UpdateAccountCmd;
import org.apache.cloudstack.api.command.admin.domain.UpdateDomainCmd;
import org.apache.cloudstack.api.command.admin.user.DeleteUserCmd;
import org.apache.cloudstack.api.command.admin.user.UpdateUserCmd;

import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;

public interface RegionManager {

    /**
     * Returns the Id of local Region
     * @return
     */
    public int getId();

    /**
     * Adds a peer Region to the local Region
     * @param id
     * @param name
     * @param endPoint
     * @return Returns added Region object
     */
    Region addRegion(int id, String name, String endPoint);

    /**
     * Update details of the Region with specified Id
     * @param id
     * @param name
     * @param endPoint
     *
     * @param apiKey
     * @param secretKey
     * @return Returns update Region object
     */
    Region updateRegion(int id, String name, String endPoint);

    /**
     * @param id
     * @return True if region is successfully removed
     */
    boolean removeRegion(int id);

    /** List all Regions or by Id/Name
     * @param id
     * @param name
     * @return List of Regions
     */
    List<RegionVO> listRegions(Integer id, String name);

    /**
     * Deletes a user by userId and propagates the change to peer Regions
     *
     * @param accountId
     *            - id of the account do delete
     *
     * @return true if delete was successful, false otherwise
     */
    boolean deleteUserAccount(long accountId);

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
     * @param accountName
     * @param domainId
     * @param id
     * @param lockRequested
     * @return
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    Account disableAccount(String accountName, Long domainId, Long id, Boolean lockRequested) throws ConcurrentOperationException, ResourceUnavailableException;

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
     * update an existing domain
     * 
     * @param cmd
     *            - the command containing domainId and new domainName
     * @return Domain object if the command succeeded
     */
    Domain updateDomain(UpdateDomainCmd updateDomainCmd);

    /**
     * Deletes domain by Id
     * @param id
     * @param cleanup
     * @return true if delete was successful, false otherwise
     */
    boolean deleteDomain(Long id, Boolean cleanup);

    /**
     * Update a user by userId
     *
     * @param userId
     * @return UserAccount object
     */
    UserAccount updateUser(UpdateUserCmd updateUserCmd);

    /**
     * Disables a user by userId
     *
     * @param userId
     *            - the userId
     * @return UserAccount object
     */	
    UserAccount disableUser(Long id);

    /**
     * Enables a user
     *
     * @param userId
     *            - the userId
     * @return UserAccount object
     */
    UserAccount enableUser(long userId);
}
