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

import com.cloud.configuration.ResourceCount;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.AccountVO;
import com.cloud.utils.component.Manager;

/**
 * AccountManager includes logic that deals with accounts, domains, and users.
 *
 */
public interface AccountManager extends Manager {

	/**
	 * Finds all ISOs that are usable for a user. This includes ISOs usable for the user's account and for all of the account's parent domains.
	 * @param userId
	 * @return List of IsoVOs
	 */
	// public List<VMTemplateVO> findAllIsosForUser(long userId);
	
	/**
	 * Finds the resource limit for a specified account and type. If the account has an infinite limit, will check
	 * the account's parent domain, and if that limit is also infinite, will return the ROOT domain's limit.
	 * @param account
	 * @param type
	 * @return resource limit
	 */
	public long findCorrectResourceLimit(AccountVO account, ResourceType type);
	
	/**
	 * Increments the resource count
	 * @param accountId
	 * @param type
	 * @param delta
	 */
	public void incrementResourceCount(long accountId, ResourceType type, Long...delta);
    
	/**
	 * Decrements the resource count
	 * @param accountId
	 * @param type
	 * @param delta
	 */
    public void decrementResourceCount(long accountId, ResourceType type, Long...delta);
	
	/**
	 * Checks if a limit has been exceeded for an account
	 * @param account
	 * @param type
	 * @return true if the limit has been exceeded
	 */
	public boolean resourceLimitExceeded(AccountVO account, ResourceCount.ResourceType type);
	
	/**
	 * Gets the count of resources for a resource type and account
	 * @param account
	 * @param type
	 * @return count of resources
	 */
	public long getResourceCount(AccountVO account, ResourceType type);
	
	/**
	 * Updates an existing resource limit with the specified details. If a limit doesn't exist, will create one.
	 * @param domainId
	 * @param accountId
	 * @param type
	 * @param max
	 * @return
	 * @throws InvalidParameterValueException
	 */
	public ResourceLimitVO updateResourceLimit(Long domainId, Long accountId, ResourceType type, Long max) throws InvalidParameterValueException;
	
}
